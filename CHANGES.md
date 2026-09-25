# Changelog

All notable Thaumic Energistics Unofficial Deconstructed changes are grouped by the version in which they
first appeared. The original mod's history is in
[its own changelog](https://github.com/Delfayne/ThaumicEnergistics/blob/main/CHANGELOG.md).

## Important compatibility notice

- Thaumic Energistics Unofficial Deconstructed runs only with AE2 Unofficial Deconstructed, not with standard
  AE2 or AE2 Unofficial Extended Life.
- Back up the world before installing or updating the mod.

## Unreleased

- **Essentia is a kind of content an ME network holds.** It is registered with AE2UD the way blocks and items
  are registered with the game, so everything written to carry any kind of content carries essentia without
  knowing what it is: the terminals list it, the key-type picker offers it, and each aspect is drawn with
  Thaumcraft's own icon in its own colour. A byte of a storage cell holds eight essentia and a machine
  operation moves one, the original mod's numbers, so nothing stored before the move stops fitting.
- **AE2UD's own buses move essentia.** An import bus draws it out of a jar, an alembic or any other Thaumcraft
  essentia container, an export bus and an interface fill one, and a storage bus mounts one as storage, so the
  original's essentia buses and essentia storage bus are gone. A jar's label is honoured. Thaumcraft cannot
  say what an insert would do without doing it, so what a jar or a tube buffer would take is worked out from
  what it holds; a container that takes essentia one at a time is filled one at a time. Essentia only ever
  comes out of a crucible or a centrifuge, since the crucible counts what it is given as taken and keeps none
  of it.
- **Phials and jars fill and empty at a terminal.** Clicking an essentia row with an empty phial or a jar in
  hand fills it from the network, and clicking with a full one empties it in, as a bucket does for a fluid;
  with an empty hand the terminal borrows a phial from storage. It works in every AE2UD terminal and on a
  filter slot, not only in an essentia terminal of its own. A phial fills and empties ten at a time, as
  Thaumcraft makes it; a jar takes up to 250 of one aspect and keeps to its label. An interface slot holds 250
  essentia, one jar's worth, before the interface's own multiplier.
- **Aspects from the recipe viewer are essentia.** With Thaumic JEI installed, an aspect dragged out of HEI
  sets a filter slot to that essentia, the recipe and usage keys work on an essentia row of a terminal, and a
  recipe that lists aspects carries them into a pattern. A phial or a jar dropped on a filter slot sets it to
  what it holds, as a bucket does for a fluid.
- **The Coalescence and Diffusion Cores and the Arcane Charging Card are back**, with the original's ids, arcane
  recipes and research. The card is an AE2UD card of its own kind now, so the Network Tool carries it without
  the original's patch to AE2; what takes it arrives with the Arcane Crafting Terminal and the Arcane
  Assembler.
- **Essentia cells from 1k to 16384k, and portable ones.** A cell is an essentia component in an essentia cell
  housing, put together from the two only, and an empty cell comes apart into them again in hand. The components
  stay arcane crafts, each from three of the size below, and the housing is one too, where the original used
  AE2's plain cell housing; there is no one-step shaped recipe, which would have skipped the arcane housing. A cell holds twelve aspects, as the
  original's did, with the same bytes per aspect and idle drain, and takes the inverter, sticky, equal
  distribution and void cards a fluid cell takes. The 256k and larger cells are there only when AE2UD's high
  capacity storage is. Portable essentia cells are new: AE2UD's portable cell holding essentia, for every size
  whose portable fluid cell is enabled. The 1k to 64k cells and components keep the original's ids and
  textures; the rest are drawn from AE2UD's in the original's colours for now.
- **The creative essentia cell holds every aspect without end**, 2^52 of each where the original held two
  billion, and swallows whatever is put in.
- **The Infusion Provider is back**, with the original's id, model and infusion recipe. It offers the whole
  network's essentia to whatever draws on essentia sources around it: a runic matrix while it infuses, an
  essentia mirror, an essentia output. What it holds is read from the network's running count, since those ask
  far more often than they take. A storage bus no longer mounts it as a container: on its own network that
  would count the network twice, and AE2UD's interfaces already join one network to another.
  Goggles of revealing show the network's essentia above it, as they do a jar's; the original sent that only as
  the chunk loaded or the block was clicked, and it is now kept up to date once a second. The One Probe and
  WAILA show whether it is online or missing a channel, as they do for AE2UD's own machines.
- **The Arcane Crafting Terminal is back**, with the original's id, recipe and research, rebuilt on AE2UD's
  crafting terminal: its list, its HEI "+" with the missing ingredients marked, and its refill from the network
  on shift-click. It crafts what the arcane workbench crafts, plain recipes included, and beside the list it shows
  Thaumcraft's own workbench at its own size, drawn from Thaumcraft's texture: the grid inside the ring of crystal
  sockets and the result box. The crystals are no longer put in by hand: an arcane recipe takes the ones it needs
  straight from the network, each glowing in its socket as on the workbench, and the ones the network is short of
  glow red; hovering one tells how many the recipe takes and how many the network holds. The vis the recipe
  costs, after the player's discount, and what the aura holds are written under the workbench, and the result
  shows only once both the vis and the crystals are there. The original's armour slots are gone: what the player
  wears still lowers the cost. The Arcane Charging Card in its slot over the result lets it draw vis from the
  eight chunks around its own, as a vis charger does for the workbench. With Thaumic JEI, the "+" on an arcane
  workbench recipe fills the grid too.
- **The wireless terminal has an arcane mode.** Crafting an Arcane Crafting Terminal into AE2UD's wireless
  terminal adds it, as the other terminals are added, in place of the original's wireless essentia terminal and
  its arcane crafting. It is the same screen, with the vis taken from the aura where the player stands at the
  moment of each craft, so it cannot be read in one chunk and spent in another. The grid is kept in the terminal.
  The Arcane Charging Card has its own slot there too, kept with the grid.
- **Arcane patterns replace the Knowledge Core and the Arcane Inscriber.** Every AE2UD pattern terminal has an
  arcane mode, which shows Thaumcraft's own workbench beside the window as the Arcane Crafting Terminal does. The
  player draws the recipe's nine squares; the crystals it takes are read off the recipe and written into the
  pattern, and so is the player, whose research decides what can be encoded: a recipe they have not researched
  gives no pattern. What the grid makes, the crystals and the vis show before encoding, and the recipe screen's
  "+" on an arcane workbench recipe opens the mode. A pattern holds one recipe where a core held nine, and can take
  substitutes and let the network fill containers as AE2UD's crafting patterns do. It is laid out as the workbench
  is, five across with the crystals down the sides, which is how the network hands it to a machine and how the
  pattern view draws it. Only an Arcane Assembler runs one.
- **The Arcane Assembler is back**, with the original's id, model, window, infusion recipe and research, rebuilt
  as AE2UD's molecular assembler is: set beside an ME interface, it takes the arcane patterns the interface holds,
  and the interface pushes each craft's ingredients and crystals into it and gets back what it made and what the
  recipe left. It waits until the aura holds the recipe's whole vis, at its full price, takes it all at once as the
  craft starts, and crafts one at a time as fast as a molecular assembler with the same acceleration cards, five
  of them. An Arcane Charging Card, in the same column as the cards, lets it draw from the eight chunks around
  its own. Its window shows what it is crafting, the crystals the craft takes, what the craft costs and what the
  aura holds, with a network tool's cards beside it when the player carries one, and the item turns inside the
  block while it works. It is the arcane workbench's catalyst in the recipe screen. The original's knowledge
  core slot and armour slots are gone with the cores. The One Probe and WAILA show whether it is online.
- **Tubes connect to AE2UD's ME Interface**, the block and the part alike. An interface set to stock essentia in
  its slots is a source tubes draw that essentia from, as from a jar, even while a tube has just emptied it;
  one that stocks none takes whatever a tube brings straight into the network, pulling harder than any tube so
  nothing flows back out. A tube meets a cable bus only on a face with an interface part. Thaumcraft looks for
  its tube interface on the block entity itself, so a mixin adds it to AE2UD's interface block and cable bus,
  which ask the interface on that face.
- **The Essentia Interface is gone.** Import and export buses on a jar, a tube buffer or an alembic do what it
  did: a tube fills a buffer the import bus empties into the network, and the export bus fills one a tube draws
  from. Its research entry goes with it.
- **Forked from [Thaumic Energistics](https://github.com/Delfayne/ThaumicEnergistics) and aimed at AE2UD.**
  The original is built on AE2 UEL, where every kind of content needs a storage channel and parts of its own,
  and it carried an essentia copy of AE2's terminal, buses, storage bus and level emitter. AE2UD has one of
  each that takes any registered kind of content, so those copies go and essentia is registered as a kind
  instead. The code built on AE2 UEL is removed in this first step and comes back rewritten, one feature at a
  time.
- **The build is the same as the other AE2UD addons'.** CleanroomMC's ForgeDevEnv, the version read from the
  latest `vX.Y.Z` git tag, a build on every push and jars published to GitHub Releases on a tag. The jar is
  called `thaumicenergistics-ud`, so it cannot be mistaken for the mod it was forked from.
- **The Russian translation is removed.**
- **No version range on AE2UD yet.** AE2UD shares its mod id with AE2 and AE2 UEL, so only a version range
  tells them apart, and the range cannot be written until AE2UD's first release.
