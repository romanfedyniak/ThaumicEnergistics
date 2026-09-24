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
