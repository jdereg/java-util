package com.cedarsoftware.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReflectionUtils has no security switches of its own (4.119.0). Through 4.118.0 six system properties could ask it
 * to refuse a list of class names ("dangerous" classes) and any class with a field named like a secret. They are
 * now ignored: with every one of them set as it used to be set, ReflectionUtils answers as it does with none.
 * <p>
 * This class is outside {@code com.cedarsoftware.util} on purpose. The old checks let through any caller whose
 * class name began with that package, so only from out here did they refuse anything: on 4.118.0 each test below
 * fails with a {@code SecurityException}.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class ReflectionUtilsSwitchesRemovedTest {
    private static final String[] SWITCHES = {
            "reflectionutils.security.enabled",
            "reflectionutils.dangerous.class.validation.enabled",
            "reflectionutils.sensitive.field.validation.enabled",
            "reflectionutils.log.obfuscation.enabled",
    };
    private static final String DANGEROUS_CLASSES = "reflectionutils.dangerous.class.patterns";
    private static final String SENSITIVE_FIELDS = "reflectionutils.sensitive.field.patterns";

    private final Map<String, String> before = new LinkedHashMap<>();

    /** Classes of this test alone, so nothing ReflectionUtils kept from another test can stand in for an answer. */
    static class Account {
        String user;
        String password;
    }

    static class Note {
        String title;
        boolean isPrivate;
        String body;
    }

    static class Memo {
        String title;
        String apiKey;
    }

    static class Vault extends Memo {
        String secretKey;
    }

    @BeforeEach
    void switchEverythingOn() {
        for (String name : SWITCHES) {
            before.put(name, System.getProperty(name));
            System.setProperty(name, "true");
        }
        before.put(DANGEROUS_CLASSES, System.getProperty(DANGEROUS_CLASSES));
        before.put(SENSITIVE_FIELDS, System.getProperty(SENSITIVE_FIELDS));
    }

    @AfterEach
    void restore() {
        for (Map.Entry<String, String> entry : before.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    @Test
    void aClassOnTheOldListAnswersLikeAnyOther() {
        // The default list named Runtime, Process, ProcessBuilder, two Unsafe classes and two script classes
        Method processors = ReflectionUtils.getMethod(Runtime.class, "availableProcessors");
        assertThat(processors).isNotNull();
        assertThat(ReflectionUtils.call(Runtime.getRuntime(), "availableProcessors"))
                .isEqualTo(Runtime.getRuntime().availableProcessors());
        assertThat(ReflectionUtils.call(Runtime.getRuntime(), processors))
                .isEqualTo(Runtime.getRuntime().availableProcessors());
        assertThat(ReflectionUtils.getMethod(Runtime.getRuntime(), "availableProcessors", 0)).isNotNull();
        assertThat(ReflectionUtils.getMethod(Runtime.class, "exec", String.class)).isNotNull();
        assertThat(ReflectionUtils.getNonOverloadedMethod(Runtime.class, "getRuntime")).isNotNull();

        Constructor<ProcessBuilder> constructor = ReflectionUtils.getConstructor(ProcessBuilder.class, List.class);
        assertThat(constructor).isNotNull();
        assertThat(ReflectionUtils.getAllConstructors(ProcessBuilder.class)).hasSize(2);
        assertThat(names(ReflectionUtils.getAllDeclaredFields(ProcessBuilder.class))).contains("command");
        assertThat(names(ReflectionUtils.getDeclaredFields(ProcessBuilder.class))).contains("command");
    }

    @Test
    void aClassWithAFieldNamedLikeASecretKeepsAllItsFields() {
        // The default words: password, secret, apikey, credential, private, ... matched anywhere in the name,
        // so 'isPrivate' counted. One such field used to cost the class every field it has.
        assertThat(names(ReflectionUtils.getAllDeclaredFields(Account.class))).containsExactly("user", "password");
        assertThat(names(ReflectionUtils.getDeclaredFields(Account.class))).containsExactly("user", "password");
        assertThat(ReflectionUtils.getField(Account.class, "password")).isNotNull();
        assertThat(ReflectionUtils.getField(Account.class, "user")).isNotNull();

        assertThat(names(ReflectionUtils.getAllDeclaredFields(Note.class)))
                .containsExactly("title", "isPrivate", "body");
        assertThat(ReflectionUtils.getAllDeclaredFieldsMap(Note.class)).containsKeys("title", "isPrivate", "body");

        // ... and a superclass with one is walked like any other
        assertThat(names(ReflectionUtils.getAllDeclaredFields(Vault.class)))
                .containsExactly("secretKey", "title", "apiKey");
    }

    @Test
    void aListOfTheCallersOwnIsIgnoredToo() {
        System.setProperty(DANGEROUS_CLASSES, Currency.class.getName() + "," + Memo.class.getName());
        System.setProperty(SENSITIVE_FIELDS, "title,user");

        // A filter of this test's own, so nothing kept from an earlier call answers for it. (Not synthetic: the
        // coverage agent adds a field to every class it instruments.)
        Predicate<Field> everyField = field -> !field.isSynthetic();
        assertThat(ReflectionUtils.getDeclaredFields(Currency.class, everyField)).isNotEmpty();
        assertThat(ReflectionUtils.getAllDeclaredFields(Currency.class, everyField)).isNotEmpty();
        assertThat(ReflectionUtils.getMethod(Currency.class, "getCurrencyCode")).isNotNull();
        assertThat(ReflectionUtils.call(Currency.getInstance("USD"), "getCurrencyCode")).isEqualTo("USD");

        assertThat(names(ReflectionUtils.getDeclaredFields(Memo.class, everyField))).containsExactly("title", "apiKey");
        assertThat(ReflectionUtils.getField(Memo.class, "title")).isNotNull();
        assertThat(ReflectionUtils.getConstructor(Memo.class)).isNotNull();
    }

    private static List<String> names(List<Field> fields) {
        List<String> names = new ArrayList<>(fields.size());
        for (Field field : fields) {
            names.add(field.getName());
        }
        return names;
    }
}
