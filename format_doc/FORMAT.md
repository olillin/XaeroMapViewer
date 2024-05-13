# Xaero's World Map file format

> This document is a work in progress and is due to change and may contain errors.

A complete documentation of the reverse engineered Xaero's World Map file format.

## Folder structure

<details>
<summary><code>XaeroWorldMap</code> folder structure</summary>

```tree
XaeroWorldMap
├───<save name>
│   │   server_config.txt
│   │
│   └───<null|DIM<int>>
│       │   .lock
│       │   dimension_config.txt
│       │
│       ├───cache
│       │   └───<int>
│       │           <x>_<y>.xwmc
│       │           <x>_<y>.xwmc.outdated
│       │
│       ├───cache_<int>
│       │       <x>_<y>.xwmc
│       │       <x>_<y>.xwmc.outdated
│       │
│       └───caves
│
└───Multiplayer_<server ip>
    │   server_config.txt
    │
    ├───DIM<int>
    │   │   dimension_config.txt
    │   │
    │   └───mw$<int>
    │       │   .lock
    │       │
    │       └───caves
    │           └───<int>
    │               │   <x>_<y>.zip
    │               │   region.xaero
    │               │
    │               ├───cache
    │               │   └───<int>
    │               │           <x>_<y>.xwmc
    │               │           <x>_<y>.xwmc.outdated
    │               │
    │               └───cache_<int>
    │                   <x>_<y>.xwmc
    │                   <x>_<y>.xwmc.outdated
    │
    └───null
        │   dimension_config.txt
        │
        └───mw$<int>
            │   <x>_<y>.zip
            │   .lock
            │
            ├───cache
            │   └───<int 1..>
            │           <x>_<y>.xwmc
            │           <x>_<y>.xwmc.outdated
            │
            ├───cache_<int 1..>
            │       <x>_<y>.xwmc
            │       <x>_<y>.xwmc.outdated
            │
            └───caves
                └───<int>
                    │   <x>_<y>.zip
                    │
                    └───cache_<int>
                            <x>_<y>.xwmc
                            <x>_<y>.xwmc.outdated
```

</details>

## .xwmc and .xwmc.outdated

Zip archives containing a singular [cache.xaero](#cachexaero) file. The acronym
stands for Xaero's World Map Cache.

## cache.xaero

*cache.xaero* is a binary file. It starts with an undeciphered. Following this
there is some sort of biome data as you can see the biome keys. Directly after
the last biome key the first "[segment](#segments)" starts.

### File signature — 4 bytes

All *cache.xaero* files start with the 4 bytes `00 01 00 18`, or 65,560 if
represented as an integer. This is the
[file signature](https://en.wikipedia.org/wiki/File_format#Magic_number).

### Undeciphered bytes

### Biome index

There is a list of biomes immediately before the first [segment](#segments). It
starts with a single byte indicating how many biomes are listed. After this
every biome is listed starting with 3 bytes where the first two have always been
`00` from what I've seen. The last byte could indicate the key to which the
biome corresponds to but because this key is not unique that is unlikely to be
the case. Instead it is likely the order of the biomes that acts as the key and
is referenced in the [biome block](#biome) of a segment. Following those bytes
the [namespaced id](https://minecraft.wiki/w/Resource_location) of the biome is
encoded in [UTF-8](https://en.wikipedia.org/wiki/UTF-8). The last character of
the namespaced id is immediately followed by the next biome or the first
[segment](#segments).

### Segments

Segments are a repeating part of *cache.xaero* that take up most of the file.
files and have three parts: [header](#header--11-bytes), [image](#image--16384-bytes) and
[extra](#extra--16400-or-24600-bytes)*.

\* extra is currently undeciphered, hence why it's lumped into a single category
even though it is likely to be several.

#### Header — 11 bytes

Each segment starts with an 11 byte header. The first byte specifies the X and Y
coordinate of the segment within the region where the first 4 bits are X and the
last 4 are Y. The coordinates are unsigned. Note that these are not block
coordinates. The meaning of the next 6 bytes is unknown. The final 4 bytes of
the header is an integer that tells us the length of the
[image](#image--16384-bytes) block, which is always 16,384 bytes long.

##### Example header

```hex
00 00 00 00 80 58 00 00 40 00 00
 1  2  3  4  5  6  7  8  9 10 11
```

This header tells us that the segment resides at position 0, 0 of the region
(the top left corner) and that the image block is 16,384 bytes long (which
is always true).

#### Image — 16,384 bytes

The image block contains a 64 x 64 RGB image where each pixel consists of 4
bytes in the format `RR GG BB 00`.

#### Extra — ~16,400 or ~24,600 bytes

The two halves of the first 16,384 bytes of extra appear to be very similar.
See [fc_out.txt](fc_out.txt).

Looking at the extra block at position 16,695 of
[0_0/cache.xaero](../in/cache_1/unzipped/0_0/cache.xaero) we can see that we have
the integer 2,048 which may be the length of the next block.

##### Biome

The extra block becomes roughly 8,200 bytes smaller on segments that feature a
single biome. Which points towards these segments having just a single biome
value rather than specifying the biome of each block.
