package com.mithrilmania.blocktopograph.chunk;

/**
 * Reference from Tommaso Checchi (/u/mojang_tommo), MCPE developer:
 * ~https://www.reddit.com/r/MCPE/comments/5cw2tm/level_format_changes_in_mcpe_0171_100/d9zv9s8/~
 * ABOVE LINK OUTDATED: USE THIS ONE INSTEAD: https://web.archive.org/web/20220706151501/https://www.reddit.com/r/MCPE/comments/5cw2tm/level_format_changes_in_mcpe_0171_100/d9zv9s8/
 */
public enum ChunkTag {

    DATA_3D((byte) 0x2B),
    @Deprecated
    /**
     *  The address is no longer written into since Caves & Cliffs (v1.18.0) \
     *  https://minecraft.fandom.com/wiki/Bedrock_Edition_level_format
     */
    DATA_2D((byte) 0x2D),
    @Deprecated
    DATA_2D_LEGACY_PE((byte) 0x2E),
    TERRAIN((byte) 0x2F), //SubChunkPrefix, data for blocks
    V0_9_LEGACY_TERRAIN((byte) 0x30),
    BLOCK_ENTITY((byte) 0x31),
    ENTITY((byte) 0x32),
    PENDING_TICKS((byte) 0x33),//TODO untested
    BLOCK_EXTRA_DATA((byte) 0x34),//TODO untested, 32768 bytes, used for top-snow.
    BIOME_STATE((byte) 0x35),//TODO untested
    GENERATOR_STAGE((byte) 0x36),
    VERSION_PRE16((byte) 0x76),
    VERSION((byte) 0x2c);;


    public final byte dataID;

    ChunkTag(byte dataID) {
        this.dataID = dataID;
    }

}
