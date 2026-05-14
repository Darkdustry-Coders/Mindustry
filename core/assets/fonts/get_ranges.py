import os
from fontTools.ttLib import TTFont

files = [TTFont(f) for f in os.listdir(".") if not f.endswith(".py") and os.path.isfile(f)]
ranges = []

ridx = 0
def insert_key(key):
    global ridx

    while True:
        ridxt = ridx + 1
        if ridx == len(ranges):
            ranges.append([key, key])
            break
        if ranges[ridx][0] <= key <= ranges[ridx][1]:
            break
        if key + 1 == ranges[ridx][0]:
            ranges[ridx][0] -= 1
            break
        if ranges[ridx][1] + 1 == key:
            ranges[ridx][1] += 1

            if ridxt == len(ranges):
                break
            if ranges[ridx][1] == ranges[ridxt][0]:
                ranges[ridx][1] = ranges[ridxt][1]
                del ranges[ridxt]

            break
        if ridxt != len(ranges) and key + 1 < ranges[ridxt][0] \
            and ranges[ridx][1] + 1 < key:
            ranges.insert(ridxt, [key, key])
            break
        ridx += 1


for file in files:
    for table in file["cmap"].tables:
        if not table.isUnicode():
            continue
        keys = list(table.cmap.keys())
        keys.sort()

        ridx = 0
        for key in keys:
            if key < 32:
                continue

            insert_key(key)

with open("../../src/mindustry/MdFontRanges.java", "w", encoding="utf-8") as f:
    f.write("package mindustry;\n")
    f.write("public class MdFontRanges {\n")
    f.write("    public static final class Range {\n")
    f.write("        public final int start;\n")
    f.write("        public final int end;\n")
    f.write("        public Range(int start, int end) { this.start = start; this.end = end; }\n")
    f.write("    }\n")
    f.write("    private MdFontRanges() {}\n")
    f.write("    public static final Range[] ranges = new Range[] {\n")
    for cr in ranges:
        f.write(f"        new Range({cr[0]}, {cr[1]}),\n")
    f.write("    };\n")
    f.write("}\n")
