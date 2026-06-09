# Go2 Controller Android App

This project is part of my internship work on teleoperating the Unitree Go2 robot.

The long-term goal is to control the robot through physical user motion, potentially using data from a wearable device.

For the first stage, the focus is on sending simple commands from an Android application to the robot through a communication bridge.

## Current System Architecture

Android App  
↓  
Communication Bridge / Relay  
↓  
Robot Control Interface  
↓  
Unitree Go2

## Week 1 Goal

The goal for the first week is to build a basic Android app that can send robot commands such as:

- Sit
- Stop
- Stand
- Move

At this stage, button presses in the app send command requests to a relay/server, which will later translate them into robot actions.

## Technologies

- Kotlin
- Android Studio
- Jetpack Compose
- HTTP requests
- Unitree Go2
- Future integration with ROS2 or WebRTC

## Project Status

Currently, the Android app interface is being developed and tested.

The next step is connecting the app to a communication bridge and verifying that button commands are successfully sent.