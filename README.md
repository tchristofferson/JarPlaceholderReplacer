# JarPlaceholderReplacer
Replaces placeholder strings with gives values

### Usage
Say you have the following constant in your jar file:
```
//This is in class com/tchristofferson/basictesting/Main.class as an example
private static final String TEST = "%__USER__%";
```
Lets say you want to replace the value for the TEST constant to an actual username. This can be achieved like so:
```
//You can write these bytes to a file to save the new jar file
//Constructor for Placeholder is (String classPath, String toBeReplaced, String replacement)
byte[] bytes = PlaceholderReplacer.replace(new JarFile(...), new Placeholder("com/tchristofferson/basictesting/Main.class", "%__USER__%", "tchristofferson"));
```
After calling the replace method and saving the bytes to a file, the TEST constant would now be:
```
private static final String TEST = "tchristofferson";
```

### Dependency Info
```
<dependency>
  <groupId>com.tchristofferson</groupId>
  <artifactId>JarPlaceholderReplacer</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
