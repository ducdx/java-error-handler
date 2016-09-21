package com.workable.errorhandler.matchers.retrofit;

import com.workable.errorhandler.Matcher;
import com.workable.errorhandler.MatcherFactory;
import retrofit2.adapter.rxjava.HttpException;


public class RetrofitMatcherFactory {

    private RetrofitMatcherFactory() {
        // no instances
    }

    /**
     * Creates a {@link MatcherFactory} that checks HTTP statuses
     * @return a new {@link MatcherFactory} that matches the given httpStatusCode with
     *         the Throwable's status code.
     */
    public static MatcherFactory<Integer> create() {
        return new MatcherFactory<Integer>() {
            public Matcher build(final Integer httpStatusCode) {
                return new Matcher() {
                    public boolean matches(Throwable throwable) {
                        return ((HttpException) throwable).code() == httpStatusCode;
                    }
                };
            }
        };
    }

    /**
     * Creates a {@link MatcherFactory} that checks if HTTP status is in given {@link Range}
     * @return a new {@link MatcherFactory} that checks if the given Range contains
     *         the Throwable's status code.
     */
    public static MatcherFactory<Range> createRange() {
        return new MatcherFactory<Range>() {
            public Matcher build(final Range range) {
                return new Matcher() {
                    public boolean matches(Throwable throwable) {
                        HttpException httpException = (HttpException)throwable;
                        return range.contains(httpException.code());
                    }
                };
            }
        };
    }
}
