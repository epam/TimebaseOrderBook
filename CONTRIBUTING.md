## Semantic Commit Messages

See how a minor change to your commit message style can make you a better programmer.

Format: `<issuesId><type>(<scope>): <subject>`

`<scope> and  <issuesId>`   is optional

### Example of Commit Message

```
#116 #118 feat(db): add hat wobble
^---------^  ^--^  ^-------^^-^
|            |     |         |+-> Component name  
|            |     +-> Summary in present tense.
|            |
|            +-------> Type: chore, docs, feat, fix, refactor, style, or test.
+-------> Issues id  in bug tracker                  
```

More Examples:

- `feat`: (new feature for the user, not a new feature for build script)
- `fix`: (bug fix for the user, not a fix to a build script)
- `docs`: (changes to the documentation)
- `style`: (formatting, missing semi colons, etc; no production code change)
- `refactor`: (refactoring production code, eg. renaming a variable)
- `test`: (adding missing tests, refactoring tests; no production code change)
- `chore`: (updating grunt tasks etc; no production code change)
