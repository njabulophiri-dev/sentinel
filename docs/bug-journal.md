# Date: 12 October 2026

*Symptom:* Output looked correct but two test lines never ran checkAmount.

*Hypothesis:* I was printing decisions I built by hand, not decisions the rule produced.

*What I tried:* Traced each println in main to see which ones called checkAmount.

*Root cause:* Test inputs bypassed the code under test.

*Fix:* Looped over amounts (199.99, 5000.00, 5000.01, 60000.00) through checkAmount.

*Lesson in one sentence:* A test only proves something if it runs the code under test.

*Layman's version:* Looking at a smoke alarm isn't the same as pressing the test button.

# 13 Oct 2026: New node never attached
- *Symptom:* putAmount said it added Zanele but findAmount returned -1.
- *Hypothesis:* The new node wasn't part of the chain.
- *What I tried:* Traced where newNode went after it was created.
- *Root cause:* Created the node but never set current.next to it.
- *Fix:* current.next = newNode; then return.
- *Lesson:* Creating an object doesn't put it where you need it. You must connect it.
- *Layman's version:* Building a carriage and leaving it in the yard instead of coupling it to the train.
