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
package com.sap.rediscart.jalo.promotion.action;

import de.hybris.platform.jalo.ConsistencyCheckException;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloBusinessException;
import de.hybris.platform.jalo.JaloInvalidParameterException;
import de.hybris.platform.jalo.JaloOnlyItem;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.product.Product;
import de.hybris.platform.jalo.security.JaloSecurityException;
import de.hybris.platform.jalo.type.ComposedType;
import de.hybris.platform.jalo.type.JaloAbstractTypeException;
import de.hybris.platform.jalo.type.JaloGenericCreationException;
import de.hybris.platform.jalo.type.TypeManager;
import de.hybris.platform.promotions.jalo.AbstractPromotionAction;
import de.hybris.platform.promotions.jalo.PromotionResult;
import de.hybris.platform.servicelayer.internal.jalo.order.JaloOnlyItemHelper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * @author Henter Liu (henterji@163.com)
 */
@SuppressWarnings("deprecation")
public class RedisRuleBasedOrderEntryAdjustAction extends GeneratedRedisRuleBasedOrderEntryAdjustAction implements JaloOnlyItem
{
	@SuppressWarnings("unused")
	private final static Logger LOG = Logger.getLogger(RedisRuleBasedOrderEntryAdjustAction.class.getName());

	private JaloOnlyItemHelper data;

	@Override
	protected Item createItem(final SessionContext ctx, final ComposedType type, final ItemAttributeMap allAttributes)
			throws JaloBusinessException
	{
		final Set missing = new HashSet();
		if (!checkMandatoryAttribute(AbstractPromotionAction.MARKEDAPPLIED, allAttributes, missing))
		{
			throw new JaloInvalidParameterException("missing parameters " + missing + " to create a cart ", 0);
		}
		final Class cl = type.getJaloClass();
		try
		{
			final RedisRuleBasedOrderEntryAdjustAction newOne = (RedisRuleBasedOrderEntryAdjustAction) cl.newInstance();
			newOne.setTenant(type.getTenant());
			newOne.data = new JaloOnlyItemHelper(//
					(de.hybris.platform.core.PK) allAttributes.get(PK), //
					newOne, //
					type, //
					new Date(), //
					null//
			);
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
	public Boolean isMarkedApplied(final SessionContext ctx)
	{
		return data.getProperty(ctx, MARKEDAPPLIED);
	}

	@Override
	public void setMarkedApplied(final SessionContext ctx, final Boolean markedApplied)
	{
		data.setProperty(ctx, MARKEDAPPLIED, markedApplied);
	}

	@Override
	public String getGuid(final SessionContext ctx)
	{
		return data.getProperty(ctx, GUID);
	}

	@Override
	public void setGuid(final SessionContext ctx, final String guid)
	{
		data.setProperty(ctx, GUID, guid);
	}

	@Override
	public PromotionResult getPromotionResult(final SessionContext ctx)
	{
		return data.getProperty(ctx, PROMOTIONRESULT);
	}

	@Override
	public void setPromotionResult(final SessionContext ctx, final PromotionResult promotionResult)
	{
		data.setProperty(ctx, PROMOTIONRESULT, promotionResult);
	}

	@Override
	public BigDecimal getAmount(final SessionContext ctx)
	{
		return data.getProperty(ctx, AMOUNT);
	}

	@Override
	public void setAmount(final SessionContext ctx, final BigDecimal amount)
	{
		data.setProperty(ctx, AMOUNT, amount);
	}

	@Override
	public Integer getOrderEntryNumber(final SessionContext ctx)
	{
		return data.getProperty(ctx, ORDERENTRYNUMBER);
	}

	@Override
	public void setOrderEntryNumber(final SessionContext ctx, final Integer orderEntryNumber)
	{
		data.setProperty(ctx, ORDERENTRYNUMBER, orderEntryNumber);
	}

	@Override
	public Product getOrderEntryProduct(final SessionContext ctx)
	{
		return data.getProperty(ctx, ORDERENTRYPRODUCT);
	}

	@Override
	public void setOrderEntryProduct(final SessionContext ctx, final Product orderEntryProduct)
	{
		data.setProperty(ctx, ORDERENTRYPRODUCT, orderEntryProduct);
	}

	@Override
	public Long getOrderEntryQuantity(final SessionContext ctx)
	{
		return data.getProperty(ctx, ORDERENTRYQUANTITY);
	}

	@Override
	public void setOrderEntryQuantity(final SessionContext ctx, final Long orderEntryQuantity)
	{
		data.setProperty(ctx, ORDERENTRYQUANTITY, orderEntryQuantity);
	}

	@Override
	protected AbstractPromotionAction deepClone(final SessionContext ctx)
	{
		try
		{
			final Map values = this.getAllAttributes(ctx);

			// Remove 'standard' values that cannot be set
			values.remove(Item.PK);
			values.remove(Item.MODIFIED_TIME);
			values.remove(Item.CREATION_TIME);
			values.remove("savedvalues");
			values.remove("customLinkQualifier");
			values.remove("synchronizedCopies");
			values.remove("synchronizationSources");
			values.remove("alldocuments");
			values.remove(Item.TYPE);
			values.remove(Item.OWNER);
			values.remove(PROMOTIONRESULT);

			// Clone subclass specific values
			deepCloneAttributes(ctx, values);

			final ComposedType type = TypeManager.getInstance().getComposedType(RedisRuleBasedOrderEntryAdjustAction.class);
			try
			{
				return (AbstractPromotionAction) type.newInstance(ctx, values);
			}
			catch (final JaloGenericCreationException ex)
			{
				LOG.warn("deepClone [" + this + "] failed to create instance of " + this.getClass().getSimpleName(), ex);
			}
			catch (final JaloAbstractTypeException ex)
			{
				LOG.warn("deepClone [" + this + "] failed to create instance of " + this.getClass().getSimpleName(), ex);
			}
		}
		catch (final JaloSecurityException ex)
		{
			LOG.warn("deepClone [" + this + "] failed to getAllAttributes", ex);
		}
		return null;
	}

	//----------------------------------------------------------------------------------
	// --- JaloOnlyItem methods
	//----------------------------------------------------------------------------------

	/**
	 * Provides composed as part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final ComposedType provideComposedType()
	{
		return this.data.provideComposedType();
	}

	/**
	 * Provides creation time as part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final Date provideCreationTime()
	{
		return this.data.provideCreationTime();
	}

	/**
	 * Provides modification time as part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final Date provideModificationTime()
	{
		return this.data.provideModificationTime();
	}

	/**
	 * Provides PK part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public final de.hybris.platform.core.PK providePK()
	{
		return this.data.providePK();
	}

	/**
	 * Custom removal logic as part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
	 */
	@Override
	public void removeJaloOnly() throws ConsistencyCheckException
	{
		this.data.removeJaloOnly();
	}

	/**
	 * Custom attribute access as part of {@link de.hybris.platform.jalo.JaloOnlyItem} contract. Never call directly
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
}
