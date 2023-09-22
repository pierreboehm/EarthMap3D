# EarthMap3D

A so called topological compass in 3D.

Based on height-maps of users current location the app creates a topological 3D-view, which can be combined with compass, tracker and much more.

The app also offers a modern screen design. 
Thanks of an attentive and reliable LocationManger-implementation all needed information will be got "just by drive". 

Perfect suitable for bicyclists, bikers, drivers, hikers, walkers or everyone else who want to use it.

(NOTE: Unfortunately this project is currently on ice due to technical problems with terrain data server. 
NASA switched off one satelite, so terrain data server cannot generate data files as usual.
NEWS: I found an alternative API that provides data similar to latest source. To make it run again I "only" need to adapt the modified terrain measurement consiting of scope of terrain (means high of satelite during shot) and interpretation of (high profile) bitmap pixel colors (colors between 0..255 represent altitude in relation to a predefined measurement range))
