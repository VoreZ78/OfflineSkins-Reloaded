<div align="center">

# OfflineSkins Reloaded

**Offline skins & capes for Minecraft**

Fabric · 26.2

<br>

[Modrinth](https://modrinth.com/mod/offlineskins-reloaded) ·
[Discord](https://discord.com/invite/KgscJEE5B) ·
[GitHub](https://github.com/VoreZ78/OfflineSkins-Reloaded)

</div>

---

OfflineSkins Reloaded is a client-side Fabric mod that keeps player skins and capes available even when Mojang services are unavailable or when playing offline.

**Heavily reworked and optimized from the original OfflineSkins project.**

---


## 1.2.0

> **A major rewrite focused on performance, configuration, caching, reliability and life better.**

### What's new

|     | Feature                   | Description                                                                |
| --- | ------------------------- | -------------------------------------------------------------------------- |
| 💾  | **Remember Skin / Cape**  | Preserve selected cached skins and capes between sessions                  |
| 🔁  | **Reset Skin / Cape**     | Quickly reset remembered skin or cape selections                           |
| ⚡   | **FSCS**                  | Fast Skin and Cape Selection                                               |
| 🔄  | **FSCRS**                 | Fast Skin and Cape Recache System                                          |
| 👤  | **Smooth Recache**        | Recache players individually instead of forcing unnecessary global updates |
| ⏱️  | **Appearance Delay**      | Configurable delay for smoother texture appearance                         |
| 📐  | **Maximum HD Resolution** | Configure the maximum supported HD texture resolution                      |
| 🐞  | **Debug Options**         | Additional tools for debugging and troubleshooting                         |
| ❓   | **FAQ**                   | Integrated FAQ menu for common questions and problems                      |
| ⚡   | **Network Optimization**  | Reduced unnecessary network traffic and improved validation flow           |
| 🖼️ | **LSC-F**                 | Legacy Skin and Cape Filter for older texture formats                      |

---

# REWRITTEN FROM SCRATCH

### `URLConnectionValidator`

The connection validation system was completely rewritten from scratch.

The new implementation provides redesigned connection checks, response handling and validation logic for skin and cape sources.

### `YaclSettings`

The YACL settings system was completely rewritten from scratch.

The configuration structure was redesigned to provide a cleaner and more maintainable settings architecture.

---

# IMPROVED AND FIXED

### `URLConnectionValidation`

Improved connection validation, response handling and error detection for external skin and cape sources.

### HD Skin Network Traffic

Reduced unnecessary network traffic when HD skins are rejected during validation or fail to pass the configured texture filters.

### FAQ Menu

Improved and fixed FAQ menu behavior, navigation and presentation.

---

# VOREZCORE FEATURES

### 🎨 Skins & Capes

* Offline skin support
* Offline cape support
* Local texture cache loading
* HD skin and cape support
* Replacement textures for disallowed HD textures
* Modernized skin loading architecture

### ⚡ Performance

* Optimized networking
* Lightweight client-side operation
* Fast cached texture selection
* Fast recaching
* Individual player recaching
* Reduced unnecessary network requests

### ⚙️ Configuration

Powered by **YACL**.

The configuration menu includes:

* Cached skin and cape settings
* Skin and cape providers
* Custom server configuration
* HD texture settings
* Maximum HD resolution
* Appearance Delay
* Remember Skin / Cape
* Reset Skin / Cape
* Debug Options
* FAQ

---

# OPENING THE CONFIGURATION

The OfflineSkins Reloaded menu can be opened using any of these methods:

| Method           | Action               |
| ---------------- | -------------------- |
| 🖥️ Title Screen | Configuration button |
| ⏸️ Pause Screen  | Configuration button |
| ⌨️ Keybind       | `U` by default       |
| 💬 Command       | `/offlineskins menu` |

The keybind can be changed through Minecraft's **Controls** menu.

---

# CACHE SYSTEM

OfflineSkins Reloaded uses a local texture cache:

```text
cachedImages/
├── skins/
└── capes/
```

### Skins

Place skin textures inside:

```text
cachedImages/skins/
```

Example:

```text
cachedImages/skins/Steve.png
```

### Capes

Place cape textures inside:

```text
cachedImages/capes/
```

Example:

```text
cachedImages/capes/Steve.png
```

The filename must match the player's username.

---

# HD TEXTURES

OfflineSkins Reloaded supports high-resolution skins and capes.

HD textures can be enabled through the `allowHD` setting for supported servers.

The maximum allowed resolution can be configured through **Maximum HD Resolution**.

When HD textures are not allowed, oversized textures can be replaced with:

```text
SkinHDNotAllowed.png
CapeHDNotAllowed.png
```

This helps avoid unnecessary network traffic from high-resolution textures that cannot be used.

The local cache can contain textures using supported resolutions.

---

# LEGACY FILTERS

OfflineSkins Reloaded includes **LSC-F — Legacy Skin and Cape Filter**.

| Legacy format | Modern format |
| ------------- | ------------- |
| `64x32` skin  | `64x64` skin  |
| `22x17` cape  | `64x32` cape  |

The textures are remapped without quality loss.

---

# CUSTOM PROVIDERS

OfflineSkins Reloaded supports custom external skin and cape providers.

Custom servers can be configured directly from the YACL configuration menu, including provider-specific HD texture permissions and server settings.

---

# SMART INTERNET CHECK

**Smart Internet Check** detects when the internet connection is unavailable and prevents unnecessary network requests.

Local textures from `cachedimages` are not affected.

Cached skins and capes can also be enabled or disabled independently.

---

# FAST SKIN & CAPE SYSTEM

### FSCS

**Fast Skin and Cape Selection** improves the process of selecting cached player textures and avoids unnecessary work during texture changes.

### FSCRS

**Fast Skin and Cape Recache System** provides smoother texture recaching by updating players individually.

### Appearance Delay

**Appearance Delay** controls how quickly newly loaded textures become visible, helping make texture updates feel smoother.

---

# VALIDATION

OfflineSkins Reloaded includes validation for custom skin and cape sources.

The validation system helps handle:

* Unavailable servers
* Unstable connections
* Provider responses

This reduces the chance of unsupported image data being applied as player skins or capes.

---

# COMMANDS

### Open configuration

```text
/offlineskins menu
```

Opens the OfflineSkins Reloaded configuration menu.

### Show version

```text
/offlineskins version
```

Displays the installed OfflineSkins Reloaded version.

---

# DEBUG OPTIONS

A dedicated **Debug Options** section is available for advanced troubleshooting.

It can be used to diagnose provider, loading, validation and other client-side issues.

---

# FAQ

OfflineSkins Reloaded includes an integrated FAQ menu covering common questions and problems related to skins, capes, caching, providers and configuration.

---

# LANGUAGES

**17 languages** are supported.

Localization files cover the configuration menu and its available options.

|     | Language |
| --- | -------- |
| 🌍  | **English** |
| 🌍  | **Russian** |
| 🌍  | **Spanish** |
| 🌍  | **German** |
| 🌍  | **French** |
| 🌍  | **Italian** |
| 🌍  | **Japanese** |
| 🌍  | **Korean** |
| 🌍  | **Polish** |
| 🌍  | **Portuguese (Brazil)** |
| 🌍  | **Simplified Chinese** |
| 🌍  | **Ukrainian** |
| 🌍  | **Azerbaijani** |
| 🌍  | **Turkish** |
| 🌍  | **Czech** |
| 🌍  | **Dutch** |
| 🌍  | **Swedish** |

<sub>⚠️ The translations are not 100% correct.</sub>

---

# ORIGINAL PROJECT

OfflineSkins Reloaded is based on the original OfflineSkins project by **zlainsama**.

**Original repository**

https://github.com/zlainsama/OfflineSkins

---

# OFFLINESKINS RELOADED

**Author:** VoreZ

**Repository**

https://github.com/VoreZ78/OfflineSkins-Reloaded

---

# CONTENT WARNING

> ⚠️ OfflineSkins Reloaded may load skins and capes from third-party services.
>
> Third-party services may contain user-created content that can be provocative, offensive, disturbing or otherwise inappropriate.
>
> This content is **not controlled, moderated or endorsed** by the OfflineSkins Reloaded project.
>
> Use third-party skin and cape sources at your own discretion.

---

<div align="center">

### OfflineSkins Reloaded 1.2.0

**Offline textures. Better caching. Better networking. Better life**

Made by **VoreZ**

</div>
