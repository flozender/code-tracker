/**
 * 
 */
package org.junit.experimental.theories.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.internal.runners.model.TestClass;

public class Assignments {
	private List<PotentialAssignment> fAssigned;

	private final List<ParameterSignature> fUnassigned;

	private final Class<?> fClass;

	public Assignments(List<PotentialAssignment> assigned,
			List<ParameterSignature> unassigned, Class<?> type) {
		fUnassigned= unassigned;
		fAssigned= assigned;
		fClass= type;
	}

	public static Assignments allUnassigned(Method testMethod,
			Class<?> testClass) throws Exception {
		List<ParameterSignature> signatures;
		signatures= ParameterSignature.signatures(new TestClass(testClass)
				.getConstructor());
		signatures.addAll(ParameterSignature.signatures(testMethod));
		return new Assignments(new ArrayList<PotentialAssignment>(),
				signatures, testClass);
	}

	public boolean isComplete() {
		return fUnassigned.size() == 0;
	}

	public ParameterSignature nextUnassigned() {
		return fUnassigned.get(0);
	}

	public Assignments assignNext(PotentialAssignment source) {
		List<PotentialAssignment> assigned= new ArrayList<PotentialAssignment>(
				fAssigned);
		assigned.add(source);

		return new Assignments(assigned, fUnassigned.subList(1, fUnassigned
				.size()), fClass);
	}

	public Object[] getActualValues(int start, int stop, boolean nullsOk)
			throws CouldNotGenerateValueException {
		Object[] values= new Object[stop - start];
		for (int i= start; i < stop; i++) {
			Object value= fAssigned.get(i).getValue();
			if (value == null && !nullsOk)
				throw new CouldNotGenerateValueException();
			values[i - start]= value;
		}
		return values;
	}

	public List<PotentialAssignment> potentialsForNextUnassigned()
			throws InstantiationException, IllegalAccessException {
		ParameterSignature unassigned= nextUnassigned();
		return getSupplier(unassigned).getValueSources(unassigned);
	}
	
	// TODO: (Dec 7, 2007 9:49:54 AM) push up default supplier

	public ParameterSupplier getSupplier(ParameterSignature unassigned)
			throws InstantiationException, IllegalAccessException {
		ParameterSupplier supplier= getAnnotatedSupplier(unassigned);
		if (supplier != null)
			return supplier;

		return new AllMembersSupplier(fClass);
	}

	public ParameterSupplier getAnnotatedSupplier(ParameterSignature unassigned)
			throws InstantiationException, IllegalAccessException {
		ParametersSuppliedBy annotation= unassigned
				.findDeepAnnotation(ParametersSuppliedBy.class);
		if (annotation == null)
			return null;
		return annotation.value().newInstance();
	}

	public Object[] getConstructorArguments(boolean nullsOk)
			throws CouldNotGenerateValueException {
		return getActualValues(0, getConstructorParameterCount(), nullsOk);
	}

	public Object[] getMethodArguments(boolean nullsOk, Object target)
			throws CouldNotGenerateValueException {
		// TODO: (Dec 7, 2007 10:08:32 AM) don't need target

		return getActualValues(getConstructorParameterCount(), fAssigned.size(), nullsOk);
	}

	public Object[] getAllArguments(boolean nullsOk) throws CouldNotGenerateValueException {
		return getActualValues(0, fAssigned.size(), nullsOk);
	}

	private int getConstructorParameterCount() {
		List<ParameterSignature> signatures= ParameterSignature.signatures(new TestClass(fClass)
				.getConstructor());
		int constructorParameterCount= signatures.size();
		return constructorParameterCount;
	}
}