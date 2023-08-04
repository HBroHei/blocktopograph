package com.mithrilmania.blocktopograph.block;

import com.mithrilmania.blocktopograph.block.icon.BlockIcon;
import com.mithrilmania.blocktopograph.block.icon.NoBlockIcon;
import com.mithrilmania.blocktopograph.block.icon.TexPathBlockIcon;

import java.io.Serializable;

public class BlockTemplate implements Serializable {

    private final String subName;

    private final Block block;

    private final BlockIcon icon;

    private final int color;

    private final boolean hasBiomeShading;

    /**
     * Create a block template
     * @param subName The identifier (ID) of the block (e.g. structure_block)
     * @param block The `Block` object of the block
     * @param icon The image / icon used to represents the block
     * @param color The color of the block. USe `0x` prefix to indicate Hex color (in RGB / ARGB)
     * @param hasBiomeShading If the block will have biome shading (I somewhat dk lol)
     */
    public BlockTemplate(String subName, Block block, BlockIcon icon, int color, boolean hasBiomeShading) {
        this.subName = subName;
        this.block = block;
        this.icon = icon;
        this.color = color;
        this.hasBiomeShading = hasBiomeShading;
    }

    public String getSubName() {
        return subName;
    }

    public Block getBlock() {
        return block;
    }

    public BlockIcon getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public boolean isHasBiomeShading() {
        return hasBiomeShading;
    }
}
