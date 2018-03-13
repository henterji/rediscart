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
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.directpersistence.annotation.ForceJALO;
import de.hybris.platform.directpersistence.annotation.SLDSafe;
import de.hybris.platform.jalo.ConsistencyCheckException;
import de.hybris.platform.jalo.Item;
import de.hybris.platform.jalo.JaloBusinessException;
import de.hybris.platform.jalo.JaloInvalidParameterException;
import de.hybris.platform.jalo.JaloItemNotFoundException;
import de.hybris.platform.jalo.JaloOnlyItem;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.JaloSystemException;
import de.hybris.platform.jalo.SessionContext;
import de.hybris.platform.jalo.c2l.Currency;
import de.hybris.platform.jalo.enumeration.EnumerationValue;
import de.hybris.platform.jalo.order.AbstractOrder;
import de.hybris.platform.jalo.order.AbstractOrderEntry;
import de.hybris.platform.jalo.order.Cart;
import de.hybris.platform.jalo.order.GeneratedAbstractOrder;
import de.hybris.platform.jalo.order.delivery.DeliveryMode;
import de.hybris.platform.jalo.order.payment.PaymentInfo;
import de.hybris.platform.jalo.order.payment.PaymentMode;
import de.hybris.platform.jalo.product.Product;
import de.hybris.platform.jalo.product.Unit;
import de.hybris.platform.jalo.security.JaloSecurityException;
import de.hybris.platform.jalo.type.ComposedType;
import de.hybris.platform.jalo.type.JaloGenericCreationException;
import de.hybris.platform.jalo.type.TypeManager;
import de.hybris.platform.jalo.user.Address;
import de.hybris.platform.jalo.user.User;
import de.hybris.platform.promotions.jalo.PromotionResult;
import de.hybris.platform.promotions.jalo.PromotionsManager;
import de.hybris.platform.promotions.result.PromotionOrderResults;
import de.hybris.platform.servicelayer.internal.jalo.order.JaloOnlyItemHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.rediscart.constants.RediscartConstants;



/**
 * A redis implementation of {@link Cart}. This item is not backed by the database and will therefore not survive the
 * end of {@link JaloSession}. Apart from that it can be used as normal cart including order creation and saved carts.
 * <p/>
 * There are several options to make {@link JaloSession} use this kind of cart:
 * <h3>Set as default cart type</h3>
 * <p>
 * Change your local.properties like this:
 *
 * <pre>
 * # Default session cart type
 * # Specifies which type of cart is being created on demand by
 * # each JaloSession. Use 'RedisCart' if you don't need a
 * # database backed session cart.
 * default.session.cart.type = RedisCart
 * </pre>
 *
 * </p>
 * <h3>Specify per session</h3> For each session which has no cart yet you may specify the cart type via session
 * context:
 *
 * <pre>
 * 	JaloSession js = ...
 *
 * 	js.getSessionContext().setAttribute(&quot;RedisCart&quot;);
 * 	// or
 * 	js.getSessionContext().setAttribute(RediscartConstants.TC.REDISCART);
 * 	// or even
 * 	js.getSessionContext().setAttribute(
 * 		TypeManager.getInstance().getComposedType( RedisCart.class )
 * 	);
 *
 * 	RedisCart newCart = (RedisCart)js.getCart();
 * </pre>
 *
 * </p>
 * <b>Please note:</b> Due to its non-persistent nature this cart usually does not survive the end of its owning
 * {@link JaloSession}! Special care should be taken when using session replication across a cluster: although this cart
 * is serializable and should therefore be transfered to all other nodes we suggest to verify if updates onto the cart
 * and its entries are really synchronized. This may depend upon the chose session replication technology.
 *
 * @author Henter Liu (henterji@163.com)
 */
@SuppressWarnings("deprecation")
public class RedisCart extends GeneratedRedisCart implements JaloOnlyItem
{
	private JaloOnlyItemHelper data;

	private final List<RedisCartEntry> entries = new ArrayList<RedisCartEntry>();

	/**
	 * Returns a new wizard instance.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected Item createItem(final SessionContext ctx, final ComposedType type, final ItemAttributeMap allAttributes)
			throws JaloBusinessException
	{
		final Set<String> missing = new HashSet<String>();
		if (!checkMandatoryAttribute(AbstractOrder.USER, allAttributes, missing)
				| !checkMandatoryAttribute(AbstractOrder.CURRENCY, allAttributes, missing))
		{
			throw new JaloInvalidParameterException("missing parameters " + missing + " to create a cart ", 0);
		}

		final Class<?> cl = type.getJaloClass();
		try
		{
			final RedisCart newOne = (RedisCart) cl.newInstance();
			newOne.setTenant(type.getTenant());
			newOne.data = new JaloOnlyItemHelper(//
					(PK) allAttributes.get(PK), //
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
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected ItemAttributeMap getNonInitialAttributes(@SuppressWarnings("unused") final SessionContext ctx,
			final ItemAttributeMap allAttributes)
	{
		final ItemAttributeMap ret = new ItemAttributeMap(allAttributes);
		ret.remove(Item.PK);
		ret.remove(Item.TYPE);
		ret.remove("itemtype");
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
	// --- Methods which we had to override : replace queries
	//----------------------------------------------------------------------------------

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#getEntries(int, int)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Collection<RedisCartEntry> getEntries(final int startIdx, final int endIdx)
	{
		final List<RedisCartEntry> ret = new ArrayList<RedisCartEntry>(entries.size());
		for (final RedisCartEntry e : entries)
		{
			if (e.getEntryNumber().intValue() >= startIdx && e.getEntryNumber().intValue() <= endIdx)
			{
				ret.add(e);
			}
		}
		return ret;
	}

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#getEntry(int)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public AbstractOrderEntry getEntry(final int index) throws JaloItemNotFoundException
	{
		for (final RedisCartEntry e : entries)
		{
			if (e.getEntryNumber().intValue() == index)
			{
				return e;
			}
		}
		throw new JaloItemNotFoundException("no entry for position " + index, 0);
	}

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#getEntriesByProduct(Product)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public List<RedisCartEntry> getEntriesByProduct(final Product product)
	{
		final List<RedisCartEntry> ret = new ArrayList<RedisCartEntry>(entries.size());
		for (final RedisCartEntry e : entries)
		{
			if (product.equals(e.getProduct()))
			{
				ret.add(e);
			}
		}
		return ret;
	}

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#removeAllEntries()} for details.
	 */
	@Override
	public void removeAllEntries()
	{
		removeEntries(getSession().getSessionContext(), new HashSet<AbstractOrderEntry>(getAllEntries()));
	}

	@Override
	protected void removeEntries(final SessionContext ctx, final Set<AbstractOrderEntry> entries)
	{
		if (entries != null)
		{
			this.entries.removeAll(entries);
		}
		super.removeEntries(ctx, entries);
	}

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#removeEntry(AbstractOrderEntry)} for details.
	 */
	@Override
	public void removeEntry(final AbstractOrderEntry entry)
	{
		entries.remove(entry);
		super.removeEntry(entry);
	}

	/**
	 * Overwritten to provide redis list of entries. See {@link AbstractOrder#getAllEntries()} for details.
	 *
	 * @deprecated
	 */
	@Deprecated
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public List<RedisCartEntry> getAllEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	@Deprecated
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	@SuppressWarnings(
	{ "rawtypes", "unchecked" })
	protected void setAllEntries(final SessionContext ctx, final List entries)
	{
		super.setAllEntries(ctx, entries); // this will only remove obsolete ones
		this.entries.clear();
		if (entries != null)
		{
			this.entries.addAll(entries);
		}
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public List<AbstractOrderEntry> getEntries(final SessionContext ctx)
	{
		return Collections.unmodifiableList(entries);
	}


	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setEntries(final SessionContext ctx, final List<AbstractOrderEntry> value)
	{
		this.setAllEntries(ctx, value);
	}

	/**
	 * Overwritten to provide redis list of entries. See
	 * {@link AbstractOrder#addNewEntry(Product, long, Unit, int, boolean)} for details.
	 */
	@Override
	public AbstractOrderEntry addNewEntry(final Product prod, final long qtd, final Unit unit, final int position,
			final boolean addToPresent)
	{
		final RedisCartEntry newOne = (RedisCartEntry) super.addNewEntry(prod, qtd, unit, position, addToPresent);
		if (!entries.contains(newOne))
		{
			if (!entries.contains(newOne))
			{
				entries.add(newOne);
				Collections.sort(this.entries, ENTRY_COMP);
			}
		}
		return newOne;
	}

	private static final Comparator<RedisCartEntry> ENTRY_COMP = new Comparator<RedisCartEntry>()
	{
		@Override
		public int compare(final RedisCartEntry o1, final RedisCartEntry o2)
		{
			return o1.getEntryNumber().intValue() - o2.getEntryNumber().intValue();
		}
	};

	//----------------------------------------------------------------------------------
	// --- Methods which we had to override : avoid cached getter / setter
	//----------------------------------------------------------------------------------

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getCode(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getCode(final SessionContext ctx)
	{
		return data.getProperty(ctx, CODE);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setCode(SessionContext,String)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setCode(final SessionContext ctx, final String code)
	{
		data.setProperty(ctx, CODE, code);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setDate(SessionContext, Date)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDate(final SessionContext ctx, final Date date)
	{
		setCreationTime(date);
		setChanged(true);
	}

	@Override
	@SLDSafe
	protected void setCreationTime(final Date creationTime)
	{
		data.setCreationTime(creationTime);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getUser(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public User getUser(final SessionContext ctx)
	{
		return data.getProperty(ctx, USER);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setUser(SessionContext, User)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setUser(final SessionContext ctx, final User user)
	{
		data.setProperty(ctx, USER, user);
		setChanged(true);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getCurrency(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Currency getCurrency(final SessionContext ctx)
	{
		return data.getProperty(ctx, CURRENCY);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setCurrency(SessionContext, Currency)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setCurrency(final SessionContext ctx, final Currency curr)
	{
		data.setProperty(ctx, CURRENCY, curr);
		setChanged(true);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getPaymentStatus(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public EnumerationValue getPaymentStatus(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.PAYMENTSTATUS);
	}

	/**
	 * Overwritten to provide redis implementation. See
	 * {@link AbstractOrder#setPaymentStatus(SessionContext, EnumerationValue)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setPaymentStatus(final SessionContext ctx, final EnumerationValue ps)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.PAYMENTSTATUS, ps);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getDeliveryStatus(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public EnumerationValue getDeliveryStatus(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.DELIVERYSTATUS);
	}

	/**
	 * Overwritten to provide redis implementation. See
	 * {@link AbstractOrder#setDeliveryStatus(SessionContext, EnumerationValue)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDeliveryStatus(final SessionContext ctx, final EnumerationValue ds)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.DELIVERYSTATUS, ds);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#isNet(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isNet(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(ctx, NET, false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setNet(SessionContext, boolean)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setNet(final SessionContext ctx, final boolean net)
	{
		data.setProperty(ctx, NET, Boolean.valueOf(net));
		setChanged(true);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getDeliveryMode(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public DeliveryMode getDeliveryMode(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.DELIVERYMODE);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setDeliveryMode(SessionContext, DeliveryMode)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDeliveryMode(final SessionContext ctx, final DeliveryMode mode)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.DELIVERYMODE, mode);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getDeliveryAddress(SessionContext)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Address getDeliveryAddress(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.DELIVERYADDRESS);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setDeliveryAddress(SessionContext, Address)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDeliveryAddress(final SessionContext ctx, final Address address)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.DELIVERYADDRESS, address);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getPaymentMode(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public PaymentMode getPaymentMode(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.PAYMENTMODE);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setPaymentMode(SessionContext, PaymentMode)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setPaymentMode(final SessionContext ctx, final PaymentMode mode)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.PAYMENTMODE, mode);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getPaymentAddress(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Address getPaymentAddress(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.PAYMENTADDRESS);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setPaymentAddress(SessionContext, Address)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setPaymentAddress(final SessionContext ctx, final Address adr)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.PAYMENTADDRESS, adr);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getPaymentInfo(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public PaymentInfo getPaymentInfo(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.PAYMENTINFO);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setPaymentInfo(SessionContext, PaymentInfo)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setPaymentInfo(final SessionContext ctx, final PaymentInfo info)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.PAYMENTINFO, info);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setPaymentCosts(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setPaymentCosts(final SessionContext ctx, final double paymentCost)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.PAYMENTCOST, Double.valueOf(paymentCost));
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getPaymentCosts(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public double getPaymentCosts(final SessionContext ctx)
	{
		return data.getPropertyDouble(ctx, GeneratedAbstractOrder.PAYMENTCOST, 0.0);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setStatusInfo(SessionContext, String)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setStatusInfo(final SessionContext ctx, final String s)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.STATUSINFO, s);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getStatusInfo(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getStatusInfo(final SessionContext ctx)
	{
		return data.getProperty(ctx, GeneratedAbstractOrder.STATUSINFO);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setStatus(SessionContext, EnumerationValue)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setStatus(final SessionContext ctx, final EnumerationValue s)
	{
		data.setProperty(ctx, STATUS, s);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getStatusInfo(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public EnumerationValue getStatus(final SessionContext ctx)
	{
		return data.getProperty(ctx, STATUS);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#isCalculated(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isCalculated(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(ctx, CALCULATED, false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setCalculated(SessionContext, boolean)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setCalculated(final SessionContext ctx, final boolean calculated)
	{
		data.setProperty(ctx, CALCULATED, Boolean.valueOf(calculated));
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getTotalTaxValuesInternal(final SessionContext ctx)
	{
		return data.getProperty(ctx, TOTALTAXVALUESINTERNAL);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTotalTaxValuesInternal(final SessionContext ctx, final String value)
	{
		data.setProperty(ctx, TOTALTAXVALUESINTERNAL, value);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public String getGlobalDiscountValuesInternal(final SessionContext ctx)
	{
		return data.getProperty(ctx, GLOBALDISCOUNTVALUESINTERNAL);
	}

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setGlobalDiscountValuesInternal(final SessionContext ctx, final String value)
	{
		data.setProperty(ctx, GLOBALDISCOUNTVALUESINTERNAL, value);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setDeliveryCosts(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDeliveryCosts(final SessionContext ctx, final double deliveryCost)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.DELIVERYCOST, Double.valueOf(deliveryCost));
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getDeliveryCosts(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public double getDeliveryCosts(final SessionContext ctx)
	{
		return data.getPropertyDouble(ctx, GeneratedAbstractOrder.DELIVERYCOST, 0.0);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setTotal(SessionContext, double)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTotal(final SessionContext ctx, final double price)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.TOTALPRICE, Double.valueOf(price));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getTotal(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public double getTotal(final SessionContext ctx)
	{
		return data.getPropertyDouble(ctx, GeneratedAbstractOrder.TOTALPRICE, 0.0);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setSubtotal(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setSubtotal(final SessionContext ctx, final double price)
	{
		data.setProperty(ctx, SUBTOTAL, Double.valueOf(price));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getSubtotal(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Double getSubtotal(final SessionContext ctx)
	{
		return Double.valueOf(data.getPropertyDouble(ctx, SUBTOTAL, 0.0));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setTotalDiscounts(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTotalDiscounts(final SessionContext ctx, final double totalDiscounts)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.TOTALDISCOUNTS, Double.valueOf(totalDiscounts));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getTotalDiscounts(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Double getTotalDiscounts(final SessionContext ctx)
	{
		return Double.valueOf(data.getPropertyDouble(ctx, GeneratedAbstractOrder.TOTALDISCOUNTS, 0.0));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setTotalTax(SessionContext, double)} for
	 * details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setTotalTax(final SessionContext ctx, final double taxes)
	{
		data.setProperty(ctx, GeneratedAbstractOrder.TOTALTAX, Double.valueOf(taxes));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#getTotalTax(SessionContext)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Double getTotalTax(final SessionContext ctx)
	{
		return Double.valueOf(data.getPropertyDouble(ctx, GeneratedAbstractOrder.TOTALTAX, 0.0));
	}

	/**
	 * Overwritten to provide redis implementation. See
	 * {@link AbstractOrder#setDiscountsIncludeDeliveryCost(SessionContext,Boolean)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDiscountsIncludeDeliveryCost(final SessionContext ctx, final Boolean value)
	{
		data.setProperty(getSession().getSessionContext(), "discountsIncludeDeliveryCost", value);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#isDiscountsIncludeDeliveryCost(SessionContext)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isDiscountsIncludeDeliveryCost(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(getSession().getSessionContext(), "discountsIncludeDeliveryCost", false));
	}

	/**
	 * Overwritten to provide redis implementation. See
	 * {@link AbstractOrder#setDiscountsIncludePaymentCost(SessionContext,Boolean)} for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public void setDiscountsIncludePaymentCost(final SessionContext ctx, final Boolean value)
	{
		data.setProperty(getSession().getSessionContext(), "discountsIncludePaymentCost", value);
		setChanged(false);
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#isDiscountsIncludePaymentCost(SessionContext)}
	 * for details.
	 */
	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	public Boolean isDiscountsIncludePaymentCost(final SessionContext ctx)
	{
		return Boolean.valueOf(data.getPropertyBoolean(getSession().getSessionContext(), "discountsIncludePaymentCost", false));
	}

	/**
	 * Overwritten to provide redis implementation. See {@link AbstractOrder#setModificationTime(Date)} for details.
	 */
	@Override
	public void setModificationTime(final Date d)
	{
		data.markModified(d);
	}

	//----------------------------------------------------------------------------------
	// --- Methods which we had to override : hook into entry creation
	//----------------------------------------------------------------------------------

	@Override
	public RedisCartEntry createNewEntry(final SessionContext ctx, final ComposedType entryType, final Product p,
			final long amount, final Unit u, final int pos)
	{
		final ComposedType t = entryType != null ? entryType
				: TypeManager.getInstance().getComposedType(RediscartConstants.TC.REDISCARTENTRY);

		final Map<String, Object> values = new HashMap<String, Object>();
		values.put(AbstractOrderEntry.ORDER, this);
		values.put(AbstractOrderEntry.PRODUCT, p);
		values.put(AbstractOrderEntry.UNIT, u);
		values.put(AbstractOrderEntry.QUANTITY, Long.valueOf(amount));
		values.put(AbstractOrderEntry.ENTRYNUMBER, Integer.valueOf(pos));

		try
		{
			return (RedisCartEntry) t.newInstance(ctx, values);
		}
		catch (final Exception e)
		{
			throw new JaloSystemException(e);
		}
	}

	//----------------------------------------------------------------------------------
	// --- Business
	//----------------------------------------------------------------------------------

	@Override
	@ForceJALO(reason = ForceJALO.SOMETHING_ELSE)
	protected String getAbstractOrderEntryTypeCode()
	{
		return RediscartConstants.TC.REDISCARTENTRY;
	}

	//----------------------------------------------------------------------------------
	// --- Copy from PromotionCart
	//----------------------------------------------------------------------------------

	@Override
	public Object getAttribute(final SessionContext ctx, final String qualifier)
			throws JaloInvalidParameterException, JaloSecurityException
	{
		Object retval = null;
		if (AbstractOrderModel.ALLPROMOTIONRESULTS.equals(qualifier))
		{
			final Set<PromotionResult> results = new HashSet<PromotionResult>();
			retval = results;

			final PromotionOrderResults por = PromotionsManager.getInstance().getPromotionResults(ctx, this);
			if (por != null)
			{
				results.addAll(por.getAllResults());
			}
		}
		else
		{
			retval = super.getAttribute(ctx, qualifier);
		}
		return retval;
	}
}
