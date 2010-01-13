/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql.expression;

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression;
import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.query.IMatchQuery;
import org.eclipse.equinox.p2.query.IQuery;

public final class WrappedIQuery extends Function implements IQLConstants {

	public WrappedIQuery(Expression[] operands) {
		super(assertLength(operands, 1, 2, KEYWORD_IQUERY));
	}

	@SuppressWarnings("unchecked")
	public Object evaluate(IEvaluationContext context) {
		Object query = operands[0].evaluate(context);

		if (query instanceof IMatchQuery<?>) {
			Object value = null;
			if (operands.length > 1)
				value = operands[1].evaluate(context);
			else
				value = ExpressionFactory.THIS.evaluate(context);
			return Boolean.valueOf(((IMatchQuery<Object>) query).isMatch(value));
		}

		if (!(query instanceof IQuery<?>))
			throw new IllegalArgumentException("iquery first argument must be an IQuery instance"); //$NON-NLS-1$

		Iterator<?> iterator = null;
		if (operands.length > 1)
			iterator = operands[1].evaluateAsIterator(context);
		else
			iterator = QLFactory.EVERYTHING.evaluateAsIterator(context);

		return ((IQuery<Object>) query).perform((Iterator<Object>) iterator);
	}

	public String getOperator() {
		return KEYWORD_IQUERY;
	}
}
