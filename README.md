# didymos
Evil twins optimized for 2 vs. 1 battles in Robocode

# Overview of the strategy
## Radar
The radar constantly follows the enemy. Moreover, during the battle, there is a continuous communication between both twins in terms of the position and energy of the enemy and the position, energy and the goal of the team member.

## Movement
The exact strategy of a robot depends on the fact if it is the leading twin or not. If so, it just moves as close to the enemy as possible without touching it. If not, the robot tries to find the nearest right angle between the goal position of the other twin and the enemy and moves there, having a bit more space towards the robot. The non-leading robot tries to avoid collisions with the leader; in the case of collisions, the robot moves only if it is not a leader to guaranty as much shooting as possible.

## Gun
The gun constantly predicts the most likely next position of the robot and aims, therefore, "in the future". If the robot is rather nearby the enemy, its starts shooting with full energy. On the one hand, the right-angle positioning guaranties that friendly fire is avoided most of the time, on the other hand, the robot evaluates before firing if its bullet may touch its team member.
