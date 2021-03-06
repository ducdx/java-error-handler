# ErrorHandler
[![Download](https://api.bintray.com/packages/workable/maven/ErrorHandler/images/download.svg) ](https://bintray.com/workable/maven/ErrorHandler/_latestVersion)
[![Bintray](https://img.shields.io/bintray/v/workable/maven/ErrorHandler.svg?maxAge=2592000)](https://bintray.com/workable/maven/ErrorHandler)
[![Travis](https://travis-ci.org/Workable/java-error-handler.svg?branch=master)](https://travis-ci.org/Workable/java-error-handler)

> Error handling library for Android and Java

## Download
Download the [latest JAR](https://bintray.com/workable/maven/ErrorHandler/_latestVersion) or grab via Maven:
```xml
<dependency>
  <groupId>com.workable</groupId>
  <artifactId>error-handler</artifactId>
  <version>0.9.1</version>
  <type>pom</type>
</dependency>
```

or Gradle:

```groovy
compile 'com.workable:error-handler:0.9.1'
```


## Usage

Let's say we're building a messaging Android app that uses both the network and a local database.

We need to:

### Setup a default ErrorHandler once

 - Configure the default ErrorHandler
 - Alias errors to codes that are easier to use like Integer, String and Enum values
 - Map errors to actions to take when those errors occur (exceptions thrown)

```java
// somewhere inside MessagingApp.java

ErrorHandler
  .defaultErrorHandler()

  // Bind certain exceptions to "offline"
  .bindErrorCode("offline", errorCode -> throwable -> {
      return throwable instanceof UnknownHostException || throwable instanceof ConnectException;
  })

  // Bind HTTP 404 status to 404
  .bindErrorCode(404, errorCode -> throwable -> {
      return ((HttpException) throwable).code() == 404;
  })

  // Bind HTTP 500 status to 500
  .bindErrorCode(500, errorCode -> throwable -> {
      return ((HttpException) throwable).code() == 500;
  })

  // Bind all DB errors to a custom enumeration
  .bindErrorCodeClass(DBError.class, errorCode -> throwable -> {
      return DBError.from(throwable) == errorCode;
  })

  // Handle HTTP 500 errors
  .on(500, (throwable, errorHandler) -> {
    displayAlert("Kaboom!");
  })

  // Handle HTTP 404 errors
  .on(404, (throwable, errorHandler) -> {
    displayAlert("Not found!");
  })

  // Handle "offline" errors
  .on("offline", (throwable, errorHandler) -> {
    displayAlert("Network dead!");
  })

  // Handle unknown errors
  .otherwise((throwable, errorHandler) -> {
    displayAlert("Oooops?!");
  })

  // Always log to a crash/error reporting service
  .always((throwable, errorHandler) -> {
    Logger.log(throwable);
  });
```

### Use ErrorHandler inside catch blocks

```java
// ErrorHandler instances created using ErrorHandler#create(), delegate to the default ErrorHandler
// So it's actually a "handle the error using only defaults"
// i.e. somewhere inside MessageListActivity.java
try {
  fetchNewMessages();
} catch (Exception ex) {
  ErrorHandler.create().handle(ex);
}
```

### Override defaults when needed

```java
// Configure a new ErrorHandler instance that delegates to the default one, for a specific method call
// i.e. somewhere inside MessageListActivity.java
try {
  fetchNewMessages();
} catch (Exception ex) {
  ErrorHandler
    .create()
    .on(StaleDataException.class, (throwable, errorHandler) -> {
        reloadList();
        errorHandler.skipDefaults();
    })
    .on(404, (throwable, errorHandler) -> {
        // We handle 404 specifically on this screen by overriding the default action
        displayAlert("Could not load new messages");
        errorHandler.skipDefaults();
    })
    .on(DBError.READ_ONLY, (throwable, errorHandler) -> {
        // We could not open our database to write the new messages
        ScheduledJob.saveMessages(someMessages).execute();
        // We also don't want to log this error because ...
        errorHandler.skipAlways();
    })
    .handle(ex);
}
```

### Things to know

ErrorHandler is __thread-safe__.


## API

### Initialize

* `defaultErrorHandler()` Get the default ErrorHandler.

* `create()` Create a new ErrorHandler that is linked to the default one.

* `createIsolated()` Create a new empty ErrorHandler that is not linked to the default one.

### Configure

* `on(Matcher, Action)` Register an _Action_ to be executed if _Matcher_ matches the error.

* `on(Class<? extends Exception>, Action)` Register an _Action_ to be executed if error is an instance of `Exception`.

* `on(T, Action)` Register an _Action_ to be executed if error is bound to T, through `bindErrorCode()` or `bindErrorCodeClass()`.

* `otherwise(Action)` Register an _Action_ to be executed only if no other _Action_ gets executed.

* `always(Action)` Register an _Action_ to be executed always and after all other actions. Works like a `finally` clause.

* `skipFollowing()`  Skip the execution of any subsequent _Actions_ except those registered via `always()`.

* `skipAlways()` Skip all _Actions_ registered via `always()`.

* `skipDefaults()` Skip any default actions. Meaning any actions registered on the `defaultErrorHandler` instance.

* `bindErrorCode(T, MatcherFactory<T>)` Bind instances of _T_ to match errors through a matcher provided by _MatcherFactory_.

* `bindErrorCodeClass(Class<T>, MatcherFactory<T>)` Bind class _T_ to match errors through a matcher provided by _MatcherFactory_.

* `clear()` Clear all registered _Actions_.

### Execute

* `handle(Throwable)` Handle the given error.


## About
A common problem in software, specially in UI software is that of error handling.

One way to classify errors can be by how they relate to the [problem domain](https://en.wikipedia.org/wiki/Problem_domain). Some errors, like `network` or `database` errors are orthogonal to the problem domain and designate truly **exceptional conditions**, while others are core parts of the domain like `validation` and `authentication` errors.

Another way to classify errors is by their **scope**. Are they **common** throughout the application or **specific** to a single screen, object or even method? Think of `UnauthorizedException` versus `InvalidPasswordException`.

And let's not forget another very simple distinction between errors. Those that are known at authoring time and thus **expected** (despite of how probable is that they occur), and those that are **unknown** until runtime.

With that in mind, we usually want to:

1. have a **default** handler for every **expected** (exceptional, common or not) error
2. handle **specific** errors **as appropriate** based on where and when they occur
3. have a **default** catch-all handler for **unknown** errors
4. **override** any default handler if needed
5. keep our code **DRY**

Java, as a language, provides you with a way to do the above. By mapping exceptional or very common errors to runtime exceptions and catching them lower in the call stack, while having specific expected errors mapped to checked exceptions and handle them near where the error occurred. Still, countless are the projects where this simple strategy has gone astray with lots of errors being either swallowed or left for the catch-all `Thread.UncaughtExceptionHandler`. Moreover, it usually comes with significant boilerplate code. `ErrorHandler` however eases this practice through its fluent API, error aliases and defaults mechanism.

This library doesn't try to solve Java specific problems, although it does help with the `log and shallow` anti-pattern as it provides an opinionated and straightforward way to act inside every `catch` block.  It was created for the needs of an Android app and proved itself useful very quickly. So it may work for you as well. If you like the concept and you're developing in  _Swift_ or _Javascript_, we're baking 'em and will be available really soon.

## License
```
The MIT License

Copyright (c) 2013-2016 Workable SA

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
