/*
 * Copyright [2018] [Henter Liu]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sap.rediscart.jalo.order;

import de.hybris.platform.core.PK;
import de.hybris.platform.directpersistence.annotation.ForceJALO;
import de.hybris.platform.jalo.ConsistencyCheckException;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloBusinessException;
import de.hybris.platform.jalo.JaloInvalidParameterException;
import de.hybris.platform.jalo.JaloOnlyItem;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.order.AbstractOrder;
import de.hybris.platform.jalo.order.AbstractOrderEntry;
import de.hybris.platform.jalo.order.CartEntry;
import de.hybris.platform.jalo.order.GeneratedAbstractOrderEntry;
import de.hybris.platform.jalo.product.Product;
import de.hybris.platform.jalo.product.Unit;
import de.hybris.platform.jalo.security.JaloSecurityException;
import de.hybris.platform.jalo.type.ComposedType;
import de.hybris.platform.jalo.type.JaloGenericCreationException;
import de.hybris.platform.servicelayer.internal.jalo.order.JaloOnlyItemHelper;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * Specific redis {@link CartEntry} implementation required by {@link RedisCart}. This item is not backed by database
 * and exists only inside its owning cart. Apart from that it can be used as normal cart entry.
 *
 * @see RedisCart
 * @author Henter Liu (henterji@163.com)
 */
@SuppressWarnings("deprecation")
public class RedisCartEntry extends GeneratedRedisCartEntry implements JaloOnlyItem
{
	private JaloOnlyItemHelper data;

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected Item createItem(final SessionContext ctx, final ComposedType type, final ItemAttributeMap allAttributes)
			throws JaloBusinessException
	{
		final Set<String> missing = new HashSet<String>();
		if (!checkMandatoryAttribute(AbstractOrderEntry.PRODUCT, allAttributes, missing)
				| !checkMandatoryAttribute(AbstractOrderEntry.QUANTITY, allAttributes, missing)
				| !checkMandatoryAttribute(AbstractOrderEntry.UNIT, allAttributes, missing)
				| !checkMandatoryAttribute(AbstractOrderEntry.ORDER, allAttributes, missing))
		{
			throw new JaloInvalidParameterException("missing parameters to create a AbstractOrderEntry ( missing " + missing + ")",
					0);
		}

		if (!(allAttributes.get(AbstractOrderEntry.ORDER) instanceof AbstractOrder))
		{
			throw new JaloInvalidParameterException("Parameter " + AbstractOrderEntry.ORDER + " should be instance of AbstractOrder",
					0);
		}

		final Class<?> cl = type.getJaloClass();
		try
		{
			final RedisCartEntry newOne = (RedisCartEntry) cl.newInstance();
			newOne.setTenant(type.getTenant());
			newOne.data = new JaloOnlyItemHelper(//
					(PK) allAttributes.get(PK), //
					newOne, //
					type, //
					new Date(), //
					null//
			);
			newOne.data.setProperty(ctx, ORDER, allAttributes.get(ORDER));
			newOne.data.setProperty(ctx, GeneratedAbstractOrderEntry.ENTRYNUMBER,
					allAttributes.get(GeneratedAbstractOrderEntry.ENTRYNUMBER));
			return newOne;
		}
		catch (final ClassCastException e)
		{
			throw new JaloGenericCreationException(
					"could not instantiate wizard class " + cl + " of type " + type.getCode() + " : " + e, 0);
		}
		catch (final InstantiationException e)
		{
			throw new JaloGenericCreationException(
					"could not instantiate wizard class " + cl + " of type " + type.getCode() + " : " + e, 0);
		}
		catch (final IllegalAccessException e)
		{
			throw new JaloGenericCreationException(
					"could not instantiate wizard class " + cl + " of type " + type.getCode() + " : " + e, 0);
		}
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected ItemAttributeMap getNonInitialAttributes(@SuppressWarnings("unused") final SessionContext ctx,
			final ItemAttributeMap allAttributes)
	{
		final ItemAttributeMap ret = new ItemAttributeMap(allAttributes);
		ret.remove(Item.PK);
		ret.remove(Item.TYPE);
		ret.remove("itemtype");
		ret.remove(ORDER);
		ret.remove(GeneratedAbstractOrderEntry.ENTRYNUMBER);
		return ret;
	}


	//----------------------------------------------------------------------------------
	// --- JaloOnlyItem methods
	//----------------------------------------------------------------------------------


	/**
	 * Provides composed as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final ComposedType provideComposedType()
	{
		return this.data.provideComposedType();
	}

	/**
	 * Provides creation time as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final Date provideCreationTime()
	{
		return this.data.provideCreationTime();
	}

	/**
	 * Provides modification time as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final Date provideModificationTime()
	{
		return this.data.provideModificationTime();
	}

	/**
	 * Provides PK part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final PK providePK()
	{
		return this.data.providePK();
	}

	/**
	 * Custom removal logic as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public void removeJaloOnly() throws ConsistencyCheckException
	{
		this.data.removeJaloOnly();
	}

	/**
	 * Custom attribute access as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public Object doGetAttribute(final SessionContext ctx, final String attrQualifier)
			throws JaloInvalidParameterException, JaloSecurityException
	{
		return this.data.doGetAttribute(ctx, attrQualifier);
	}

	/**
	 * Custom attribute access as part of {@link JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public void doSetAttribute(final SessionContext ctx, final String attrQualifier, final Object value)
			throws JaloInvalidParameterException, JaloSecurityException, JaloBusinessException
	{
		this.data.doSetAttribute(ctx, attrQualifier, value);
	}

	//----------------------------------------------------------------------------------
	// --- Methods which we had to override : replace queries -> none here
	//----------------------------------------------------------------------------------

	//----------------------------------------------------------------------------------
	// --- Methods which we had to override : avoid cached getter / setter
	//----------------------------------------------------------------------------------

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getOrder(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public AbstractOrder getOrder(final SessionContext ctx)
	{
		return data.getProperty(ctx, ORDER);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setOrder(final SessionContext ctx, final AbstractOrder order)
	{
		data.setProperty(ctx, ORDER, order);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getProduct(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Product getProduct(final SessionContext ctx)
	{
		return data.getProperty(ctx, PRODUCT);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setProduct(SessionContext, Product)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setProduct(final SessionContext ctx, final Product p)
	{
		data.setProperty(ctx, PRODUCT, p);
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getQuantity(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Long getQuantity(final SessionContext ctx)
	{
		return Long.valueOf(data.getPropertyLong(ctx, QUANTITY, 0));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setQuantity(SessionContext, long)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setQuantity(final SessionContext ctx, final long qtd)
	{
		data.setProperty(ctx, QUANTITY, Long.valueOf(qtd));
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getUnit(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Unit getUnit(final SessionContext ctx)
	{
		return data.getProperty(ctx, UNIT);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setUnit(SessionContext, Unit)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setUnit(final SessionContext ctx, final Unit unit)
	{
		data.setProperty(ctx, UNIT, unit);
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getInfo(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getInfo(final SessionContext ctx)
	{
		return data.getProperty(ctx, INFO);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setInfo(SessionContext, String)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setInfo(final SessionContext ctx, final String info)
	{
		data.setProperty(ctx, INFO, info);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getBasePrice(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Double getBasePrice(final SessionContext ctx)
	{
		return Double.valueOf(data.getPropertyDouble(ctx, BASEPRICE, 0.0));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setBasePrice(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setBasePrice(final SessionContext ctx, final double price)
	{
		data.setProperty(ctx, BASEPRICE, Double.valueOf(price));
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getTotalPrice(SessionContext)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Double getTotalPrice(final SessionContext ctx)
	{
		return Double.valueOf(data.getPropertyDouble(ctx, TOTALPRICE, 0.0));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setTotalPrice(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTotalPrice(final SessionContext ctx, final double price)
	{
		data.setProperty(ctx, TOTALPRICE, Double.valueOf(price));
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getTaxValuesInternal(final SessionContext ctx)
	{
		return data.getProperty(ctx, TAXVALUESINTERNAL);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTaxValuesInternal(final SessionContext ctx, final String value)
	{
		data.setProperty(ctx, TAXVALUESINTERNAL, value);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getDiscountValuesInternal(final SessionContext ctx)
	{
		return data.getProperty(ctx, DISCOUNTVALUESINTERNAL);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDiscountValuesInternal(final SessionContext ctx, final String value)
	{
		data.setProperty(ctx, DISCOUNTVALUESINTERNAL, value);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#isCalculated(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isCalculated(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(ctx, CALCULATED, false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setCalculated(SessionContext, boolean)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setCalculated(final SessionContext ctx, final boolean calculated)
	{
		data.setProperty(ctx, CALCULATED, Boolean.valueOf(calculated));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#isGiveAway(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isGiveAway(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(ctx, GIVEAWAY, false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setGiveAway(SessionContext, boolean)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setGiveAway(final SessionContext ctx, final boolean giveaway)
	{
		data.setProperty(ctx, GIVEAWAY, Boolean.valueOf(giveaway));
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#isRejected(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isRejected(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(ctx, REJECTED, false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#setRejected(SessionContext, boolean)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setRejected(final SessionContext ctx, final boolean rejected)
	{
		data.setProperty(ctx, REJECTED, Boolean.valueOf(rejected));
		setChanged();
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrderEntry#getEntryNumber()} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Integer getEntryNumber()
	{
		return Integer.valueOf(data.getPropertyInt(null, ENTRYNUMBER, -1));// YTODO really return -1 if not set ???
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected void setEntryNumberDirect(final int nr)
	{
		data.setProperty(null, ENTRYNUMBER, Integer.valueOf(nr));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link Item#setModificationTime(Date)} for details.
	 */
	@Override
	public void setModificationTime(final Date d)
	{
		data.markModified(d);
	}
}
