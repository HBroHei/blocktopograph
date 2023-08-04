package com.mithrilmania.blocktopograph.chunk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mithrilmania.blocktopograph.BuildConfig;
import com.mithrilmania.blocktopograph.Log;
import com.mithrilmania.blocktopograph.WorldData;
import com.mithrilmania.blocktopograph.block.Block;
import com.mithrilmania.blocktopograph.block.BlockTemplate;
import com.mithrilmania.blocktopograph.block.BlockTemplates;
import com.mithrilmania.blocktopograph.chunk.terrain.TerrainSubChunk;
import com.mithrilmania.blocktopograph.map.Biome;
import com.mithrilmania.blocktopograph.map.Dimension;
import com.mithrilmania.blocktopograph.util.ColorUtil;
import com.mithrilmania.blocktopograph.util.Noise;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class BedrockChunk extends Chunk {

    private static final int POS_HEIGHTMAP = 0;
    /**
     * The byte position of the biome data in the data
     */
    private static final int POS_BIOME_DATA = 0x300;
    public static final int DATA2D_LENGTH = 0x300;

    private boolean mHasBlockLight;
    private final boolean[] mDirtyList;
    private final boolean[] mVoidList;
    private final boolean[] mErrorList;
    private boolean mIs2dDirty;
    private final TerrainSubChunk[] mTerrainSubChunks;
    // TODO Change data2D to data3D
    private volatile ByteBuffer data2D;

    private int counter = 0;

    BedrockChunk(WorldData worldData, Version version, int chunkX, int chunkZ, Dimension dimension,
                 boolean createIfMissing) {
        super(worldData, version, chunkX, chunkZ, dimension);
        mVoidList = new boolean[16];
        mErrorList = new boolean[16];
        mDirtyList = new boolean[16];
        mTerrainSubChunks = new TerrainSubChunk[16];
        load2dData(createIfMissing);
        mHasBlockLight = true;
        mIs2dDirty = false;
    }

    //TODO check mIsError / getChunkData working
    private void load2dData(boolean createIfMissing) {
        if (data2D == null) {
            try {
                byte[] rawData = mWorldData.get().getChunkData(
                        mChunkX, mChunkZ, ChunkTag.DATA_3D, mDimension, (byte) 0, false);
                /*
                Current status:
                    getChunkData() can successfully return raw data from leveldb
                    Data length seems to be consistently 1060 bytes
                        (only for flat world it seems, normal downloaded maps have actual variable sizes)
                    Datas are stored in pairs of 2 (e.g. [73,0,73,0,...])
                        which is matching what the Minecraft Wiki says:
                            Heightmap (256x2 bytes)
                            Biome data (varying lengths)
                        (from https://minecraft.fandom.com/wiki/Bedrock_Edition_level_format)
                    Each vertical chunk has 1 chunks
                    Each pairs' first number represents the maximum height that column has.
                 */
                //DEBUG MSG
                /*
                counter++;
                Log.d(this,mChunkX + "," + mChunkZ + ". RawData: " + Arrays.toString(rawData));

                // Offset to after height map: 512

                int counter16 = 0;
                int zLayer = 0;
                String outputMsg = "";
                Log.d(this,"     001     002     003     004     005     006     007     008     009     010     011     012     013     014     015     016");
                for (int i = 0; i < rawData.length; i+=2) {
                    Byte b = rawData[i];
                    Byte b2 = rawData[i+1];
                    outputMsg += String.format("%03d", b.intValue()) + " " + String.format("%03d", b2.intValue()) + "|";
                    counter16++;
                    if (counter16 >= 16) {
                        counter16 = 0;
                        Log.d(this, String.format("%03d", zLayer) + ": " + outputMsg);
                        outputMsg = "";
                        zLayer++;
                        if(counter16%256==0 && counter16!=0){
                            Log.d(this,"");
                            counter16 = 0;
                            break;
                        }

                    }
                }
                Log.d(this,mChunkX + "," + mChunkZ + ". RawData Len: " + rawData.length);

                // DEBUG FOR LOOP
                int counter73 = 0;
                int counter4 = 0;
                for(int i=0;i < rawData.length;i+=2){
                    if(rawData[i]==3 && rawData[i+1]==0)
                        counter73++;
                    else if(rawData[i]==4 && rawData[i+1]==0)
                        counter4++;
                }
                Log.d(this,mChunkX + "," + mChunkZ + ", RawData count 3: " + counter73 + " | 4: " + counter4 + " | Total: " + (counter73+counter4));
                /**/
                // END OF DEBUG CODE

                if (rawData == null) {
                    if (createIfMissing) {
                        // https://wiki.vg/Bedrock_Edition_level_format
                        // ^Is that only partly updated?
                        this.data2D = ByteBuffer.allocate(DATA2D_LENGTH);
                    } else {
                        Log.w(this,"rawData = null, createIfMissing = false");
                        Log.d(this,mChunkX + "," + mChunkZ);
                        mIsError = true;
                        mIsVoid = true;
                    }
                    return;
                }
                this.data2D = ByteBuffer.wrap(rawData);
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    Log.d(this, e);
                }
                Log.w(this,"e");
                mIsError = true;
                mIsVoid = true;
            }

            /*
            Data3D format: (According to PaLM)
                16 bytes for block state entries
                32 bytes for entity entries
                48 bytes for tile entity entries
             */
        }
    }

    @Nullable
    private TerrainSubChunk getSubChunk(int which, boolean createIfMissing) {
        if (mIsError || mVoidList[which]) return null;
        //Log.d(this,"NOT (mIsError || mVoidList[which])");
        TerrainSubChunk ret = mTerrainSubChunks[which];
        if (ret == null) {
            byte[] raw;
            WorldData worldData = mWorldData.get();
            try {
                raw = worldData.getChunkData(mChunkX, mChunkZ,
                        ChunkTag.TERRAIN, mDimension, (byte) which, true);
                //Log.d(this,mChunkX + ", " + mChunkZ + " raw: " + Arrays.toString(raw));
                if (raw == null && !createIfMissing) {
                    mVoidList[which] = true;
                    Log.d(this,"Voided (1)");
                    return null;
                }
            } catch (Exception e) {
                Log.d(this,"Errored (1)");
                if (BuildConfig.DEBUG) {
                    Log.d(this, e);
                }
                mErrorList[which] = true;
                mVoidList[which] = true;
                return null;
            }
            ret = raw == null ?
                    TerrainSubChunk.createEmpty(8) :
                    TerrainSubChunk.create(raw);
            if (ret == null || ret.isError()) {
                mVoidList[which] = true;
                mErrorList[which] = true;
                //Log.d(this, String.valueOf((ret==null)));
                ret = null;
                //Log.d(this, String.valueOf(ret.isError()));
            } else if (!ret.hasBlockLight()) mHasBlockLight = false;
            mTerrainSubChunks[which] = ret;
        }
        return ret;
    }

    private int get2dOffset(int x, int z) {
        return (z << 4) | x;
    }

    @Override
    public boolean supportsBlockLightValues() {
        return mHasBlockLight;
    }

    @Override
    public boolean supportsHeightMap() {
        return true;
    }

    @Override
    public int getHeightLimit() {
        return 321;
    }

    @Override
    public int getHeightMapValue(int x, int z) {
        if (mIsVoid) return 0;
        short h = data2D.getShort(POS_HEIGHTMAP + (get2dOffset(x, z) << 1));
        return ((h & 0xff) << 8) | ((h >> 8) & 0xff);
    }

    private void setHeightMapValue(int x, int z, short height) {
        if (mIsVoid) return;
        data2D.putShort(POS_HEIGHTMAP + (get2dOffset(x, z) << 1), Short.reverseBytes(height));
    }

    @Override
    public int getBiome(int x, int z) {
        //'Log.d(this,"Biome: Is Void: "  + mIsVoid);
        if (mIsVoid) return 0;
        //Log.d(this, String.valueOf(data2D.get(POS_BIOME_DATA + get2dOffset(x, z))));
        return data2D.get(POS_BIOME_DATA + get2dOffset(x, z));
    }

    @Override
    public void setBiome(int x, int z, int id) {
        if (mIsVoid) return;
        data2D.put(POS_BIOME_DATA + get2dOffset(x, z), (byte) id);
        mIs2dDirty = true;
    }

    private int getNoise(int x, int z) {
        // noise values are between -1 and 1
        // 0.0001 is added to the coordinates because integer values result in 0
        double xval = (mChunkX << 4) | x;
        double zval = (mChunkZ << 4) | z;
        double oct1 = Noise.noise(
                (xval / 100.0) % 256 + 0.0001,
                (zval / 100.0) % 256 + 0.0001);
        double oct2 = Noise.noise(
                (xval / 20.0) % 256 + 0.0001,
                (zval / 20.0) % 256 + 0.0001);
        double oct3 = Noise.noise(
                (xval / 3.0) % 256 + 0.0001,
                (zval / 3.0) % 256 + 0.0001);
        return (int) (60 + (40 * oct1) + (14 * oct2) + (6 * oct3));
    }

    @Override
    public int getGrassColor(int x, int z) {
        Biome biome = Biome.getBiome(getBiome(x, z) & 0xff);
        int noise = getNoise(x, z);
        //Log.d(this, "Biome Color: " + String.valueOf(getBiome(x, z) & 0xff/**/)); //TODO This should not return 255
        int r = 30 + (biome.color.red / 5) + noise;
        int g = 110 + (biome.color.green / 5) + noise;
        int b = 30 + (biome.color.blue / 5) + noise;
        return ColorUtil.truncateRgb(r, g, b);
    }

    @NonNull
    @Override
    public BlockTemplate getBlockTemplate(int x, int y, int z, int layer) {
        if (checkBlockPosLimit(x,y,z))
            return BlockTemplates.getAirTemplate();
        //Log.d(this,"It is within bound");
        //TODO Check if this works correctly
        TerrainSubChunk subChunk = getSubChunk(y >> 4, false);
        if (subChunk == null)
            return BlockTemplates.getAirTemplate();
        //Log.d(this,"Subchunk is not null");
        //Log.d(this,subChunk.getBlockTemplate(x, y & 0xf, z, layer).getBlock().getName());
        return subChunk.getBlockTemplate(x, y & 0xf, z, layer);
    }

    @NonNull
    @Override
    public Block getBlock(int x, int y, int z, int layer) {
        if (checkBlockPosLimit(x,y,z))
            throw new IllegalArgumentException();
        TerrainSubChunk subChunk = getSubChunk(y >> 4, false);
        if (subChunk == null)
            return BlockTemplates.getAirTemplate().getBlock();
        return subChunk.getBlock(x, y & 0xf, z, layer);
    }

    @Override
    public void setBlock(int x, int y, int z, int layer, @NonNull Block block) {
        if (checkBlockPosLimit(x,y,z))
            return;
        int which = y >> 4;
        TerrainSubChunk subChunk = getSubChunk(which, true);
        if (subChunk == null) return;
        subChunk.setBlock(x, y & 0xf, z, layer, block);
        mDirtyList[which] = true;
        BlockTemplate template = BlockTemplates.getBest(block);
        // Height increased.
        if (template != BlockTemplates.getAirTemplate() && getHeightMapValue(x, z) < y) {
            mIs2dDirty = true;
            setHeightMapValue(x, z, (short) (y + 1));
            // Roof removed.
        } else if (template == BlockTemplates.getAirTemplate() && getHeightMapValue(x, z) == y) {
            mIs2dDirty = true;
            int height = 0;
            for (int h = y - 1; h >= 0; h--) {
                if (getBlockTemplate(x, h, z) != BlockTemplates.getAirTemplate()) {
                    height = h + 1;
                    break;
                }
            }
            setHeightMapValue(x, z, (short) height);
        }
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        if (!mHasBlockLight || x >= 16 || y >= 321 || z >= 16 || x < 0 || y < -64 || z < 0 || mIsVoid)
            return 0;
        TerrainSubChunk subChunk = getSubChunk(y >> 4, false);
        if (subChunk == null) return 0;
        return subChunk.getBlockLightValue(x, y & 0xf, z);
    }

    @Override
    public int getSkyLightValue(int x, int y, int z) {
        if (checkBlockPosLimit(x,y,z))
            return 0;
        TerrainSubChunk subChunk = getSubChunk(y >> 4, false);
        if (subChunk == null) return 0;
        return subChunk.getSkyLightValue(x, y & 0xf, z);
    }

    @Override
    public int getHighestBlockYUnderAt(int x, int z, int y) {
        if (checkBlockPosLimit(x,y,z))
            return -1;
        TerrainSubChunk subChunk;
        for (int which = y >> 4; which >= 0; which--) {
            subChunk = getSubChunk(which, false);
            if (subChunk == null) continue;
            for (int innerY = (which == (y >> 4)) ? y & 0xf : 15; innerY >= 0; innerY--) {
                if (subChunk.getBlockTemplate(x, innerY, z, 0) != BlockTemplates.getAirTemplate())
                    return (which << 4) | innerY;
            }
        }
        return -1;
    }

    @Override
    public int getCaveYUnderAt(int x, int z, int y) {
        if (checkBlockPosLimit(x,y,z))
            return -1;
        TerrainSubChunk subChunk;
        for (int which = y >> 4; which >= 0; which--) {
            subChunk = getSubChunk(which, false);
            if (subChunk == null) continue;
            for (int innerY = (which == (y >> 4)) ? y & 0xf : 15; innerY >= 0; innerY--) {
                if (subChunk.getBlockTemplate(x, innerY, z, 0) == BlockTemplates.getAirTemplate())
                    return (which << 4) | innerY;
            }
        }
        return -1;
    }

    @Override
    public void save() throws WorldData.WorldDBException, IOException {

        if (mIsError || mIsVoid) return;

        WorldData worldData = mWorldData.get();
        if (worldData == null)
            throw new RuntimeException("World data is null.");

        // Save biome and hightmap.
        if (mIs2dDirty)
            worldData.writeChunkData(
                    mChunkX, mChunkZ, ChunkTag.DATA_3D, mDimension, (byte) 0, false, data2D.array());

        // Save subChunks.
        for (int i = 0, mTerrainSubChunksLength = mTerrainSubChunks.length; i < mTerrainSubChunksLength; i++) {
            TerrainSubChunk subChunk = mTerrainSubChunks[i];
            if (subChunk == null || mVoidList[i] || !mDirtyList[i]) continue;
            //Log.d(this,"Saving "+i);
            subChunk.save(worldData, mChunkX, mChunkZ, mDimension, i);
        }
    }

    private boolean checkBlockPosLimit(int x,int y, int z){
        return (x >= 16 || y >= 320 || z >= 16 || x < 0 || y < -64 || z < 0 || mIsVoid);
    }
}
