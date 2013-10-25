Thesis-Application
==================
This project includes a part of the the application developed for my M.Eng thesis. 
Detailed description here: http://web.mit.edu/aeroastro/labs/halab/papers/Thesis-Radhika.pdf

The overall goal for this project was to develop an application for Boeing to use in their assembly line to track tasks 
accomplished during a shift in their manufacturing operations and re-plan tasks when worker availabilities change during
a shift.

There are 2 main components in this project--
- A scheduler that solves a constraint satisfaction problem using backtracking search (contained in the package src/hal/taskscheduler/model/)
- An interface prototype for an Android 10" tablet. The interface is structured according by functionality: the main activity (MainActivity.java) is in folder src/hal/taskscheduler, all screen dialog windows are in the folder src/hal/taskscheduler/dialogs and all component listeners are in the src/hal/taskscheduler/listeners folder.

Dummy data was generated to demo this application and can be found in the assets/ folder.

The application currently does not have a persistent store and hence data does not persist between application sessions.
