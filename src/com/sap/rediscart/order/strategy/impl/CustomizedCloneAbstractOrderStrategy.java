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
package com.sap.rediscart.order.strategy.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.order.AbstractOrderEntryTypeService;
import de.hybris.platform.order.strategies.ordercloning.impl.DefaultCloneAbstractOrderStrategy;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAddProductActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAdjustTotalActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderChangeDeliveryModeActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderEntryAdjustActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedPotentialPromotionMessageActionModel;
import de.hybris.platform.promotions.model.CachedPromotionOrderEntryConsumedModel;
import de.hybris.platform.promotions.model.CachedPromotionResultModel;
import de.hybris.platform.promotions.model.PromotionOrderEntryConsumedModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.servicelayer.internal.model.impl.ItemModelCloneCreator;
import de.hybris.platform.servicelayer.internal.model.impl.ItemModelCloneCreator.CopyContext;
import de.hybris.platform.servicelayer.type.TypeService;

import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderAddProductActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderAdjustTotalActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderChangeDeliveryModeActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderEntryAdjustActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedPotentialPromotionMessageActionModel;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedCloneAbstractOrderStrategy extends DefaultCloneAbstractOrderStrategy
{
	private final TypeService typeService;
	private final ItemModelCloneCreator itemModelCloneCreator;
	private final AbstractOrderEntryTypeService abstractOrderEntryTypeService;

	/**
	 * @param typeService
	 * @param itemModelCloneCreator
	 * @param abstractOrderEntryTypeService
	 */
	public CustomizedCloneAbstractOrderStrategy(final TypeService typeService, final ItemModelCloneCreator itemModelCloneCreator,
			final AbstractOrderEntryTypeService abstractOrderEntryTypeService)
	{
		super(typeService, itemModelCloneCreator, abstractOrderEntryTypeService);
		this.typeService = typeService;
		this.itemModelCloneCreator = itemModelCloneCreator;
		this.abstractOrderEntryTypeService = abstractOrderEntryTypeService;
	}

	@Override
	public <T extends AbstractOrderModel> T clone(final ComposedTypeModel _orderType, final ComposedTypeModel _entryType,
			final AbstractOrderModel original, final String code, final Class abstractOrderClassResult,
			final Class abstractOrderEntryClassResult)
	{
		validateParameterNotNull(original, "original must not be null!");
		validateParameterNotNull(abstractOrderClassResult, "abstractOrderClassResult must not be null!");
		validateParameterNotNull(abstractOrderEntryClassResult, "abstractOrderEntryClassResult must not be null!");

		final ComposedTypeModel orderType = getOrderType(_orderType, original, abstractOrderClassResult);
		final ComposedTypeModel entryType = getOrderEntryType(_entryType, original, abstractOrderClassResult,
				abstractOrderEntryClassResult);

		final CopyContext copyContext = new CopyContext()
		{
			@Override
			public ComposedTypeModel getTargetType(final ItemModel originalModel)
			{
				if (originalModel instanceof AbstractOrderEntryModel)
				{
					return entryType;
				}
				// promotion results
				if (originalModel instanceof CachedPromotionResultModel)
				{
					return typeService.getComposedTypeForClass(PromotionResultModel.class);
				}
				if (originalModel instanceof CachedPromotionOrderEntryConsumedModel)
				{
					return typeService.getComposedTypeForClass(PromotionOrderEntryConsumedModel.class);
				}
				// promotion actions
				if (originalModel instanceof RedisRuleBasedOrderAddProductActionModel)
				{
					return typeService.getComposedTypeForClass(RuleBasedOrderAddProductActionModel.class);
				}
				if (originalModel instanceof RedisRuleBasedOrderAdjustTotalActionModel)
				{
					return typeService.getComposedTypeForClass(RuleBasedOrderAdjustTotalActionModel.class);
				}
				if (originalModel instanceof RedisRuleBasedOrderChangeDeliveryModeActionModel)
				{
					return typeService.getComposedTypeForClass(RuleBasedOrderChangeDeliveryModeActionModel.class);
				}
				if (originalModel instanceof RedisRuleBasedOrderEntryAdjustActionModel)
				{
					return typeService.getComposedTypeForClass(RuleBasedOrderEntryAdjustActionModel.class);
				}
				if (originalModel instanceof RedisRuleBasedPotentialPromotionMessageActionModel)
				{
					return typeService.getComposedTypeForClass(RuleBasedPotentialPromotionMessageActionModel.class);
				}
				return super.getTargetType(originalModel);
			}
		};

		final T orderClone = (T) itemModelCloneCreator.copy(orderType, original, copyContext);
		if (code != null)
		{
			orderClone.setCode(code);
		}
		postProcess(original, orderClone);
		return orderClone;
	}

	private <T extends AbstractOrderModel> ComposedTypeModel getOrderType(final ComposedTypeModel orderType,
			final AbstractOrderModel original, final Class<T> clazz)
	{
		if (orderType != null)
		{
			return orderType;
		}

		if (clazz.isAssignableFrom(original.getClass()))
		{
			return typeService.getComposedTypeForClass(original.getClass());
		}

		return typeService.getComposedTypeForClass(clazz);
	}

	private <E extends AbstractOrderEntryModel, T extends AbstractOrderModel> ComposedTypeModel getOrderEntryType(
			final ComposedTypeModel entryType, final AbstractOrderModel original, final Class<T> orderClazz, final Class<E> clazz)
	{
		if (entryType != null)
		{
			return entryType;
		}

		if (orderClazz.isAssignableFrom(original.getClass()))
		{
			return abstractOrderEntryTypeService.getAbstractOrderEntryType(original);
		}

		return typeService.getComposedTypeForClass(clazz);
	}
}
