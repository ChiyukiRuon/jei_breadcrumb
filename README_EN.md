# JEI Breadcrumb

[简体中文](README.md) | English

A [JEI](https://github.com/mezz/JustEnoughItems) addon that adds a breadcrumb bar to the top of JEI's recipe screen, so you can see how you got to the recipe you are looking at and step back through it.

---

## Usage

Clicking any item inside the JEI recipe screen adds it as a step in the breadcrumb.

- Clicking an item **inside the recipe screen** continues the current path
- Clicking an item in the **ingredient list sidebar** or in the **bookmarks** starts a fresh path

> [!NOTE]
> This is a client-side mod. Installing it on the client is enough; installing it on a server does nothing.

### Key bindings

The breadcrumb buttons can all be bound to keys. They are **unbound by default**; set them in `Options → Controls → Key Binds → JEI Breadcrumb`.

Holding back or forward repeats.

---

## Recipe and Uses

Each step is outlined in the color of the lookup that created it:

- **Green**: you looked up how to make that item, so it is a recipe output
- **Orange**: you looked up what that item is used for, so it is an ingredient

---

## Configuration

`config/jei_breadcrumb-client.toml`

| Option        | Type      | Default     | Description                                                    |
|---------------|-----------|-------------|----------------------------------------------------------------|
| `enabled`     | `boolean` | `true`      | Whether to show the breadcrumb bar                             |
| `recipeColor` | `string`  | `"#4CAF50"` | Color of the "Recipe" marker, for the icon outline and tooltip |
| `usesColor`   | `string`  | `"#E08A2E"` | Color of the "Uses" marker, for the icon outline and tooltip   |

> [!NOTE]
> Colors use the `#RRGGBB` format; an invalid value is reset to the default.

---

## Commands

### `/jeib debug`

Controls whether debug logging is written **for the current game run**.

| Command               | Description                     |
|-----------------------|---------------------------------|
| `/jeib debug`         | Show whether it is currently on |
| `/jeib debug enable`  | Turn debug logging on           |
| `/jeib debug disable` | Turn debug logging off          |

> [!NOTE]
> This is a client command. The setting only lasts for the current run: it goes back to off when the game restarts.

---

## Credits

- [mezz](https://github.com/mezz)
- [deepseek](https://deepseek.com/en/)

## License

[MIT](LICENSE)
