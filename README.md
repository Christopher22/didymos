# Didymos
## Introduction
One of the crucial parts in the implementation of a rational agent is its optimization for
the conditions it should survive. The robot Didymos (Greek transcription of the word
’twin’), programmed in Java, is an example of such an optimized agent. Constantly
improved for Robocode battles in a team against a single enemy, our inspiration for the
concept of ”evil twins” were the examples commonly found in history and our modern pop
culture. After the ”Jekyll & Hyde” motive firstly published by Robert Louis Stevenson in
1886 (Stevenson, 1886), evil twins nowadays ”are basically captivating an evil reflection
of our favorite characters, who usually look just like them but often have more sinister
motives” (Barton, 2014). Even the history provides some examples of evil twins: The
”Spahalski Brothers” (Walsh & Jorgensen, 2017) were identical twins and both killers,
but each acted alone and unaware of the other twin’s murderous acts. Instead of this
horrible and macabre ”inspiration,” the Robocode robots will highly collaborate in the
battle.

## Strategies
Both Didymos instances are intelligent agents, which will behave rationally and reason-
ably. Unlike its predecessor Eureka, the robot was completely rewritten and simplified
into a single file. Reasons for this are the experiences gained with the somewhat com-
plicated ancestor which performed quite unfavorable in contrast to the high amount of
strategies implemented in it. In the current twins, there is no longer any learning or
further adaptation to the environment but rather a precoded strategy described in the
following.

The actual movement strategy of the individual twin robots depends on the fact if it is
leading or not. While both robots share the same code base, in some situations it seems to
be more efficient to resolve those by using the current energy as the measurement of power
to bias decision making in favor of the robot which may be the actual pillar of a victory.
The leading twin according to this analysis will move as close to the enemy as possible,
nevertheless avoiding any collision resulting in an unnecessary loss of existential energy.
On the other hand, its partner will try to find the nearest right angle between the goal
position of the other twin and the enemy, maintaining a more significant distance to the
latter one. Due to the normal movement of the enemy, collisions between the twins may
appear; it is therefore critical for the non-leading robot to avoid those with the leader.
Even if the expected outcome for such kind of ”emergency stop” may be non-optimal and
results in a longer time until the robot reaches its individual goal, this kind of self-sacrifice
will help to improve the overall performance in battle. Following the same logic, in case
of a failure in the avoidance, the ”helper robot” will move away from the leader. By this
behavior, the leading twin may continue shooting and perform their duties for the victory.

Not only for the prediction of the movement of the enemy an efficient radar strategy
is crucial. In the current implementation, the radar continually follows the opponent.
Moreover, beside this active scanning process during the battle, there is a constant com-
munication between both twins regarding the position and energy of it and the position,
power and the goal of the other twin. This exchange allows a suitable reaction even in
the unlikely case of losing the targeted enemy.

The main component of the gun strategy is a surprisingly well-working linear move-
ment prediction. Since only one enemy is present, the gun continually aims at the most
likely next position of the enemy robot. Afterwards, if Didymos is nearby the enemy,
it will start shooting with the maximal possible energy. Due to the small distance, any
advance strategies an enemy might use to fight its inferiority regarding its energy in com-
parison to the twins like i.e. bullet shielding are no longer possible; the time for reaction
is too short. The right-angle positioning strategy of the twins guaranties maximal dam-
age while friendly fire is avoided most of the time. Nevertheless, for the unlikely event
of the situation that a twin might move right before the other one, further evaluation is
performed before shooting.

## Summary
In experiments, the performance of Didymos was quite impressive. Undoubtful, this
follows from the fact that the twins may just rely on the unfair truth that the energy
level of the overall team being three times as high in comparison with the alone fighting
standard robot. Nevertheless, the results show that a handcrafted strategy may be an
efficient way of constructing a rational agent capable of handling the constraints given to
it and be more suitable than more complex solutions in some cases.

## References
Barton, C. (2014, Feb). 18 most memorable evil twins in tv history. WhatCulture.com. Retrieved from http://whatculture.com/tv/18-memorable-evil-twins-tv-history

Stevenson, R. L. (1886). The Strange Case of Dr. Jekyll and Mr. Hyde. New York: Scribner.

Walsh, A., & Jorgensen, C. (2017). Criminology: the essentials. SAGE Publications.
