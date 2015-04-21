spoon run oracle/jdk:8,apache/ant --mount %CD% /c ant
spoon build --name=spoonbrew/todo-editor --overwrite spoon.me

echo "Now we're one push away: spoon push spoonbrew/todo-editor"