# Go2 Teleoperation Project

This repository contains work in progress for my internship project on teleoperating the Unitree Go2 robot.

The final goal of the project is to control the robot through physical user motion. The original idea was to use a Meta Quest 3 headset to read motion data, but the project may instead use a wearable device, such as a smartwatch, to collect movement data from the user.

At the current stage, the focus is on building an Android application that can send simple commands to the robot, such as `Sit`, `Stand`, or `Stop`.

The planned system structure is:

```text
User Motion / App Input
        ↓
Android Application
        ↓
Communication Bridge
        ↓
Robot Control Interface
        ↓
Unitree Go2
```

This project is still in an early development phase, so the repository will be updated as the implementation progresses. More detailed documentation will be added in the `documentation/` folder.
