# This project sucks...
...but works just enough

# How to use
## Build `vciproxy.exe` from [here.](https://github.com/Sascha-T/actiaproxy)
... and place it in the root directory.

## Acquire Actia XS Evo and a Diagbox install
The `vciproxy.exe` needed to use the currently only usable adapter (Actia XS Evo) currently expects the `VCIAccess.dll` in the same location as a standard Diagbox install. \
Contributions for alternative adapters (Support for [Arduino](https://github.com/ludwig-v/arduino-psa-diag/tree/master) should be easy but I do not have a device to test with) are welcome.

## Use!
Run `gradlew run --console=plain`. \
You must now choose the adapter via `.adapter diagbox`, and subsequently the ECU with `.ecu TXH:RXH` (ex. `.ecu 752:652` for BSI). \
After this setup, you may begin to send UDS commands, and be assisted in unlocking the ECU (SA level 3) with `.unlock CODE`.
