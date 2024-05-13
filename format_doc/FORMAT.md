# Xaero's World Map file format

> This document is a work in progress and is due to change and may contain errors.

A complete documentation of the reverse engineered Xaero's World Map file format.

## Folder structure

<details>
<summary>Compacted folder structure</summary>

```
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

`cache.xaero` is a binary file. It starts with an undeciphered string of bytes.
Following this there is some sort of biome data as you can see the biome keys.
Directly after the last biome key the first "[segment](#segments)" starts.

### Segments

Segments have three parts: [header](#header), [image](#image) and
[extra](#extra).

#### Header

The header is 10 bytes long. The first byte specifies the X and Y coordinate of
the segment within the region where the first 4 bits are X and the last 4 are
Y. The coordinates are unsigned. Note that these are not block coordinates. The
byte is followed by the following constant string of 9 bytes referred to as the
`HEADER_IDENTIFIER`:

```hex
00 00 00 80 58 00 00 40 00
 1  2  3  4  5  6  7  8  9
```

####  Image

The next 16,384 bytes of the segment contain a 64 x 64 RGB image where each
pixel is 4 bytes in the format `00 RR GG BB`.

#### Extra

Data in extra is currently undeciphered, hence why it's lumped into a single
category even though it may turn out to be several.

The two halves of the first 16,384 bytes of extra appear to be very similar.
See [fc_out.txt](fc_out.txt).
