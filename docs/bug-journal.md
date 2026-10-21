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

# 14 Oct 2026: cannot find symbol: variable current
- *Symptom:* 6 compile errors, all "cannot find symbol: current".
- *Hypothesis:* The variable wasn't declared in put.
- *What I tried:* Read the first error's line number and checked the line above.
- *Root cause:* The declaration line was missing from the saved file.
- *Fix:* Added Node<K, V> current = buckets[index]; and saved.
- *Lesson:* Fix the first compile error first; the rest are often echoes.
- *Layman's version:* Calling someone by name before introducing them.

## 19 Oct 2026: Two commits missed the pull request
- *Symptom:* main was missing the .gitignore change and journal update.
- *Hypothesis:* I pushed to the branch after the PR was merged.
- *What I tried:* git log main..origin/week-1/my-hashmap to list commits main lacked.
- *Root cause:* Commits made after merging a PR don't join it.
- *Fix:* Opened a second small PR and merged it.
- *Lesson:* Before deleting a branch, check that main already has all its commits.
- *Layman's version:* Mailing a parcel, then finding two items still on your desk.

## 19 Oct 2026: No tests to run after switching branch
- *Symptom:* mvn test said "No tests to run"; it compiled 6 files instead of 8.
- *Hypothesis:* The test files weren't on this branch.
- *What I tried:* Compared the file counts in the Maven output.
- *Root cause:* Branched from main before the HashMap branch was merged into it.
- *Fix:* Merged the HashMap branch into main, then main into the CLI branch.
- *Lesson:* Merge finished work into main before starting the next branch.
- *Layman's version:* Starting a new notebook before copying the old notes into the main binder.

## 20 Oct 2026: Trailing comma slipped past the field count
- *Symptom:* "T010,Spar,Groceries," reported "got 3 fields" instead of a missing amount.
- *Hypothesis:* split(",") drops empty trailing fields.
- *What I tried:* Predicted the message, wrote a failing test then ran it.
- *Root cause:* String.split(",") removes trailing empty strings by default.
- *Fix:* line.split(",", -1) keeps them.
- *Lesson:* A library default can hide bad input; test the edges.
- *Layman's version:* A counting machine that skips empty seats at the end of a row.

## 21 Oct 2026: Compile error is not a failing test
- *Symptom:* Maven said COMPILATION ERROR at TotalsReportTest line 20.
- *Hypothesis:* A syntax mistake around line 19 or 20 from an edit.
- *What I tried:* Fixed the file until it compiled again.
- *Root cause:* Not recorded. The file wasn't committed, so I can't see what changed.
- *Fix:* File compiled again after my edits.
- *Lesson:* A compile error means no tests ran. Commit working code before experimenting, so a diff can show what broke.
- *Layman's version:* A smoke alarm with no battery: silence doesn't mean there's no fire.

