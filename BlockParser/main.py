blockIDfile = open("input.txt","r")
blockIDs = [i_rows.split("\t") for i_rows in blockIDfile.readlines()]

for blockData in blockIDs:
    print("allTemplates.put(\"minecraft:" + blockData[2] + "\", new BlockTemplate[]{"
    + "new BlockTemplate(null, new Block.Builder(BlockType.STONE).build(), new TexPathBlockIcon(\"blocks/stone.png\"), BlockColors.DEEPSLATE, false)"
    + "});")              