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
package com.sap.rediscart.promotionengineservices.action.impl;

import static java.util.Objects.isNull;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.OrderService;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAddProductActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.calculation.RuleEngineCalculationService;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;
import de.hybris.platform.ruleengineservices.rao.CartRAO;
import de.hybris.platform.ruleengineservices.rao.DiscountRAO;
import de.hybris.platform.ruleengineservices.rao.FreeProductRAO;
import de.hybris.platform.ruleengineservices.rao.OrderEntryRAO;
import de.hybris.platform.ruleengineservices.util.OrderUtils;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;


/**
 * Encapsulates logic of adding new order entry and give a 100% discount for it.
 *
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedAddProductToCartActionStrategy extends CustomizedRuleActionStrategy<RuleBasedOrderAddProductActionModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomizedAddProductToCartActionStrategy.class);

	private CartService cartService;
	private OrderService orderService;
	private ProductService productService;
	private RuleEngineCalculationService ruleEngineCalculationService;
	private OrderUtils orderUtils;

	/**
	 * If the parameter action is of type {@link DiscountRAO} and is applied to Order Entry the discount is added to
	 * corresponding Order Entry model.
	 *
	 * @return list of {@link PromotionResultModel} as a result of the {@link DiscountRAO} application.
	 */
	@Override
	public List<PromotionResultModel> apply(final AbstractRuleActionRAO action)
	{
		if (!(action instanceof FreeProductRAO))
		{
			LOG.error("cannot apply {}, action is not of type FreeProductRAO, but {}", getClass().getSimpleName(), action);
			return Collections.emptyList();
		}
		final FreeProductRAO freeAction = (FreeProductRAO) action;
		if (!(freeAction.getAppliedToObject() instanceof CartRAO))
		{
			LOG.error("cannot apply {}, appliedToObject is not of type CartRAO, but {}", getClass().getSimpleName(),
					action.getAppliedToObject());
			return Collections.emptyList();
		}

		if (freeAction.getAddedOrderEntry() == null || freeAction.getAddedOrderEntry().getProduct() == null
				|| freeAction.getAddedOrderEntry().getProduct().getCode() == null)
		{
			LOG.error("cannot apply {}, addedOrderEntry.product.code is not set.", getClass().getSimpleName());
			return Collections.emptyList();
		}

		final OrderEntryRAO addedOrderEntryRao = freeAction.getAddedOrderEntry();

		ProductModel product = null;
		try
		{
			product = getProductService().getProductForCode(addedOrderEntryRao.getProduct().getCode());
		}
		catch (UnknownIdentifierException | AmbiguousIdentifierException e)
		{
			LOG.error("cannot apply {}, product for code: {} cannot be retrieved due to exception {}.", getClass().getSimpleName(),
					addedOrderEntryRao.getProduct().getCode(), e.getClass().getSimpleName(), e);
			return Collections.emptyList();
		}

		final PromotionResultModel promoResult = getPromotionActionService().createPromotionResult(action);
		if (promoResult == null)
		{
			LOG.error("cannot apply {}, promotionResult could not be created.", getClass().getSimpleName());
			return Collections.emptyList();
		}
		final AbstractOrderModel order = promoResult.getOrder();
		if (isNull(order))
		{
			LOG.error("cannot apply {}, order or cart not found: {}", getClass().getSimpleName(), order);
			// detach the promotion result if its not saved yet.
			if (getModelService().isNew(promoResult))
			{
				getModelService().detach(promoResult);
			}
			return Collections.emptyList();
		}

		final AbstractOrderEntryModel abstractOrderEntry;
		if (order instanceof CartModel)
		{
			abstractOrderEntry = getCartService().addNewEntry((CartModel) order, product, addedOrderEntryRao.getQuantity(), null, -1,
					false);
		}
		else
		{
			abstractOrderEntry = getOrderService().addNewEntry((OrderModel) order, product, addedOrderEntryRao.getQuantity(), null,
					-1, false);
		}
		abstractOrderEntry.setGiveAway(Boolean.TRUE);
		addedOrderEntryRao.setEntryNumber(abstractOrderEntry.getEntryNumber());

		final RuleBasedOrderAddProductActionModel actionModel = createOrderAddProductAction(action,
				addedOrderEntryRao.getQuantity(), product, promoResult);
		handleActionMetadata(action, actionModel);

		getModelService().saveAll(promoResult, actionModel, order, abstractOrderEntry);
		return Collections.singletonList(promoResult);
	}

	protected RuleBasedOrderAddProductActionModel createOrderAddProductAction(final AbstractRuleActionRAO action,
			final int quantity, final ProductModel product, final PromotionResultModel promoResult)
	{
		final RuleBasedOrderAddProductActionModel actionModel = createPromotionAction(promoResult, action);
		actionModel.setProduct(product);
		actionModel.setQuantity(Long.valueOf(quantity));
		return actionModel;
	}

	@Override
	public void undo(final ItemModel item)
	{
		if (item instanceof RuleBasedOrderAddProductActionModel)
		{
			final RuleBasedOrderAddProductActionModel action = (RuleBasedOrderAddProductActionModel) item;
			handleUndoActionMetadata(action);
			final ProductModel product = action.getProduct();
			final Long quantity = action.getQuantity();
			final AbstractOrderModel order = action.getPromotionResult().getOrder();
			final AbstractOrderEntryModel undoEntry = findOrderEntryForUndo(order, action);
			if (undoEntry == null)
			{
				LOG.error("cannot undo {}, cannot find order entry for undo(). Looking for product {} with quantity {}",
						getClass().getSimpleName(), product.getCode(), quantity);
				return;
			}
			final Long newQuantity = undoEntry.getQuantity() != null
					? Long.valueOf(undoEntry.getQuantity().longValue() - quantity.longValue())
					: Long.valueOf(0L - quantity.longValue());
			if (order instanceof CartModel)
			{
				getCartService().updateQuantities((CartModel) order, new SingletonMap(undoEntry.getEntryNumber(), newQuantity));
			}
			else if (order instanceof OrderModel)
			{
				getOrderUtils().updateOrderQuantities((OrderModel) order, new SingletonMap(undoEntry.getEntryNumber(), newQuantity));
			}
			if (!getModelService().isRemoved(undoEntry))
			{
				undoInternal(action);
				getModelService().save(undoEntry);
			}
			else
			{
				// the entry has been removed, so we need to normalize the orderEntry.entryNumber attribute
				normalizeEntryNumbers(order);
			}
			recalculateIfNeeded(order);
		}
	}

	/**
	 * normalizes the given {@code order}'s entries by assigning them increasing entry numbers starting from 0. This method
	 * is called during the {@link #undo(ItemModel)} call of this action if the undo removes an order entry completely.
	 *
	 * @param order
	 *           the order to normalize
	 */
	protected void normalizeEntryNumbers(final AbstractOrderModel order)
	{
		final List<AbstractOrderEntryModel> entries = new ArrayList<AbstractOrderEntryModel>(order.getEntries());
		Collections.sort(entries, new BeanComparator(AbstractOrderEntryModel.ENTRYNUMBER, new ComparableComparator()));
		for (int i = 0; i < entries.size(); i++)
		{
			entries.get(i).setEntryNumber(Integer.valueOf(i));
			getModelService().save(entries.get(i));
		}
	}

	/**
	 * tries to find an order entry usable for the {@link #undo(ItemModel)} method based on matching product, quantity and
	 * {@code giveAway} flag: First tries to match an order entry that is marked as {@code giveAway} with the same product
	 * and quantity. If none is found, tries to find an entry with just matching product and quantity. If still none is
	 * found, tries to find an entry for the given product with higher quantity.
	 *
	 * @param order
	 *           the order to look for an order entry for
	 * @param action
	 *           the action
	 * @return an order entry or null if no matching one is found
	 */
	protected AbstractOrderEntryModel findOrderEntryForUndo(final AbstractOrderModel order,
			final RuleBasedOrderAddProductActionModel action)
	{
		// first try to find give-away entry with matching product and quantity
		final AbstractOrderEntryModel giveAwayEntry = findMatchingGiveAwayEntry(order, action);
		if (giveAwayEntry != null)
		{
			return giveAwayEntry;
		}

		// if not found, try to find an entry with matching product and quantity
		final AbstractOrderEntryModel matchingEntryWithProductAndQuantity = getEntryWithMatchingProductAndQuantity(order, action);
		if (matchingEntryWithProductAndQuantity != null)
		{
			return matchingEntryWithProductAndQuantity;
		}

		// if still not found, try to find an entry with matching product and higher quantity
		for (final AbstractOrderEntryModel entry : order.getEntries())
		{
			if (action.getProduct().equals(entry.getProduct()) && action.getQuantity().compareTo(entry.getQuantity()) < 0)
			{
				return entry;
			}
		}

		// no entry found
		return null;
	}

	/**
	 * @return if exists, an order entry with matching product and quantity, null otherwise
	 */
	protected AbstractOrderEntryModel getEntryWithMatchingProductAndQuantity(final AbstractOrderModel order,
			final RuleBasedOrderAddProductActionModel action)
	{
		for (final AbstractOrderEntryModel entry : order.getEntries())
		{
			if (action.getProduct().equals(entry.getProduct()) && action.getQuantity().equals(entry.getQuantity()))
			{
				return entry;
			}
		}
		return null;
	}

	/**
	 * @return if exists give-away order entry with matching product and quantity , null otherwise
	 */
	protected AbstractOrderEntryModel findMatchingGiveAwayEntry(final AbstractOrderModel order,
			final RuleBasedOrderAddProductActionModel action)
	{
		for (final AbstractOrderEntryModel entry : order.getEntries())
		{
			if (BooleanUtils.isTrue(entry.getGiveAway()) && action.getProduct().equals(entry.getProduct())
					&& action.getQuantity().equals(entry.getQuantity()))
			{
				return entry;
			}
		}
		return null;
	}

	protected CartService getCartService()
	{
		return cartService;
	}

	@Required
	public void setCartService(final CartService cartService)
	{
		this.cartService = cartService;
	}

	protected ProductService getProductService()
	{
		return productService;
	}

	@Required
	public void setProductService(final ProductService productService)
	{
		this.productService = productService;
	}

	protected RuleEngineCalculationService getRuleEngineCalculationService()
	{
		return ruleEngineCalculationService;
	}

	@Required
	public void setRuleEngineCalculationService(final RuleEngineCalculationService ruleEngineCalculationService)
	{
		this.ruleEngineCalculationService = ruleEngineCalculationService;
	}

	protected OrderService getOrderService()
	{
		return orderService;
	}

	@Required
	public void setOrderService(final OrderService orderService)
	{
		this.orderService = orderService;
	}

	protected OrderUtils getOrderUtils()
	{
		return orderUtils;
	}

	@Required
	public void setOrderUtils(final OrderUtils orderUtils)
	{
		this.orderUtils = orderUtils;
	}
}
