package com.tlc.validator.expression;

import com.tlc.validator.expression.model.CriteriaData;

import java.util.Collection;

/**
 * A criteria that assembles the expression based on the provided information,
 * designed for potential inheritance in their respective use cases.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface Criteria {

    /**
     * Creates a new {@link Criteria} that specifies a equal comparison of the given field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b> <span style="color:red"><b>Note: Must provide " " empty string in case of null value</b></span>
     *          Eg: firstName.equals('Lenin')
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount == 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria eq(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies a not equal comparison of the given field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b> <span style="color:red"><b>Note: Must provide " " empty string in case of null value</b></span>
     *          Eg: !firstName.equals('Fidel Castro')
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount != 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notEq(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies a startsWith comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only string can be allowed and provide " " empty string in case of null value</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg: firstName.startsWith('Karl Marx')
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria startsWith(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies does not startsWith comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only string can be allowed and provide " " empty string in case of null value</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg: !firstName.startsWith('Che Guevara')
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notStartsWith(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies endsWith comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only string can be allowed and provide " " empty string in case of null value</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg: firstName.endsWith('Fidel Castro')
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria endsWith(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies does not endsWith comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only string can be allowed and provide " " empty string in case of null value</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg: !firstName.endsWith('Karl Marx')
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notEndsWith(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies a contains comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span> <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<String> list = new ArrayList<>(List.of("Decker", "Harrison"));
     *              list.contains('Decker');
     *          </pre>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<Long> list = new ArrayList<>(List.of(1L, 2L));
     *              list.contains(1L);
     *          </pre>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria contains(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies a not contains comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span> <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<String> list = new ArrayList<>(List.of("Martin", "Kubrick"));
     *              !list.contains('Scorsese');
     *          </pre>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<Long> list = new ArrayList<>(List.of(1L, 2L));
     *              !list.contains(3L);
     *          </pre>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notContains(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies greater than equal comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only number can be allowed</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount >= 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria ge(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies less than equal comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only number can be allowed</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount <= 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria le(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies less than comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only number can be allowed</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount < 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria lt(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies greater than comparison of the given field.
     * <br/>
     * <span style="color:red"><b>Note: Only number can be allowed</b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Number}:</b>
     *          Eg: amount > 5000
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria gt(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies empty comparison of the given field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b> <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span>
     *          Eg: firstName.isEmpty
     *     </li>
     *     <li> <b>{@link Collection}:</b> <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span>
     *          Eg:
     *          <pre>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              list.isEmpty;
     *          </pre>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:<span style="color:red"><b> Empty check for number values should consider them as null</b></span>
     *     </b>
     *          Eg: amount == null
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria empty(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies not empty comparison of the given field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b> <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span>
     *          Eg: !firstName.isEmpty
     *     </li>
     *     <li> <b>{@link Collection}:</b> <span style="color:red"><b>Note: Provide an empty list in case of no value</b></span>
     *          Eg:
     *          <pre>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              !list.isEmpty;
     *          </pre>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:<span style="color:red"><b> Empty check for number values should consider them as non-null</b></span>
     *     </b>
     *          Eg: amount != null
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notEmpty(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies a containsAll comparison of the given field.
     * The values' containment must first be established in the map context of an expression
     * (<span style="color:red"><b>An empty {@link Collection} if there's no value</b></span>);
     * only then is it obligatory to check the valueKey field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Collection}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              final Collection<Sting> values = new ArrayList<>(List.of("Scorsese")); // To establish in map context.
     *              list.containsAll(values);
     *          </pre>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria containsAll(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies not containsAll comparison of the given field.
     * The values' containment must first be established in the map context of an expression
     * (<span style="color:red"><b>An empty {@link Collection} if there's no value</b></span>);
     * only then is it obligatory to check the valueKey field.
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link Collection}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              final Collection<Sting> values = new ArrayList<>(List.of("Scorsese")); // To establish in map context.
     *              !list.containsAll(values);
     *          </pre>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     */
    Criteria notContainsAll(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies is-changed comparison of the given field.
     * The previous values must be established in the map context of an expression;
     * only then is it obligatory to check the valueKey field.
     * <br/>
     * <span style="color:red"><b>
     * <ul>
     *     <li>
     *         An empty {@link Collection} in case of multi value field.
     *     </li>
     *     <li>
     *         An empty " " {@link String} in case of string field.
     *     </li>
     *     <li>
     *         {@link Number} must hold null in case of number field.
     *     </li>
     * </ul>
     * </b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          <pre>
     *              final String name = "Marx";
     *              final String oldName = "Lenin";
     *              !name.equals('Lenin'); // oldValue
     *          </pre>
     *     </li>
     *     <li> <b>{@link Collection}:</b>
     *          Eg:
     *          <code>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              final Collection<Sting> oldList = new ArrayList<>(List.of("Kubrick"));
     *              !list.containsAll(oldList);
     *          </code>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          <code>
     *              final Long value = 1L;
     *              final Long oldValue = 3L;
     *              value != 3L; // oldValue
     *          </code>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     * @see #notEq(CriteriaData) {@code notEq(final String fieldKey, final Object value)}
     */
    Criteria changed(final CriteriaData criteriaData);

    /**
     * Creates a new {@link Criteria} that specifies not-changed comparison of the given field.
     * The values to be checked must be established in the map context of an expression;
     * only then is it obligatory to check with the current field key.
     * <br/>
     * <span style="color:red"><b>
     * <ul>
     *     <li>
     *         An empty {@link Collection} in case of multi value field.
     *     </li>
     *     <li>
     *         An empty " " {@link String} in case of string field.
     *     </li>
     *     <li>
     *         {@link Number} must hold null in case of number field.
     *     </li>
     * </ul>
     * </b></span>
     * <br/>
     * <span style="color:orange"><b>For Reference:</b></span>
     * <ul>
     *     <li> <b>{@link String}:</b>
     *          <pre>
     *              final String name = "Marx";
     *              final String oldName = "Lenin";
     *              name.equals('Lenin'); // oldValue
     *          </pre>
     *     </li>
     *     <li> <b>{@link Collection}:</b>
     *          Eg:
     *          <pre>
     *              final Collection<Sting> list = new ArrayList<>(List.of("Scorsese", "Kubrick"));
     *              final Collection<Sting> oldList = new ArrayList<>(List.of("Kubrick"));
     *              list.containsAll(oldList);
     *          </pre>
     *     </li>
     *     </li>
     *     <li> <b>{@link Number}:</b>
     *          <pre>
     *              final Long value = 1L;
     *              final Long oldValue = 3L;
     *              value == 3L; // oldValue
     *          </pre>
     *     </li>
     * </ul>
     *
     * @param criteriaData Refers the {@link CriteriaData}.
     * @return The criteria of an expression.
     * @see #eq(CriteriaData) {@code eq(final String fieldKey, final Object value)}.
     */
    Criteria notChanged(final CriteriaData criteriaData);
}
