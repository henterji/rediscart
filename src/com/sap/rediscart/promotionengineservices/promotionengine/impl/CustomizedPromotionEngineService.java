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
package com.sap.rediscart.promotionengineservices.promotionengine.impl;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.order.AbstractOrder;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAddProductActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderAdjustTotalActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderChangeDeliveryModeActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderEntryAdjustActionModel;
import de.hybris.platform.promotionengineservices.model.RuleBasedPotentialPromotionMessageActionModel;
import de.hybris.platform.promotionengineservices.promotionengine.impl.DefaultPromotionEngineService;
import de.hybris.platform.promotions.jalo.PromotionResult;
import de.hybris.platform.promotions.jalo.PromotionsManager.AutoApplyMode;
import de.hybris.platform.promotions.model.AbstractPromotionActionModel;
import de.hybris.platform.promotions.model.CachedPromotionOrderEntryConsumedModel;
import de.hybris.platform.promotions.model.CachedPromotionResultModel;
import de.hybris.platform.promotions.model.PromotionGroupModel;
import de.hybris.platform.promotions.model.PromotionOrderEntryConsumedModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.promotions.result.PromotionOrderResults;
import de.hybris.platform.servicelayer.internal.model.ModelCloningContext;
import de.hybris.platform.servicelayer.internal.model.ModelCloningStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import com.sap.rediscart.model.order.RedisCartModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderAddProductActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderAdjustTotalActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderChangeDeliveryModeActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedOrderEntryAdjustActionModel;
import com.sap.rediscart.model.promotion.action.RedisRuleBasedPotentialPromotionMessageActionModel;
import com.sap.rediscart.util.RedisKeyGenerator;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedPromotionEngineService extends DefaultPromotionEngineService
{
	private RedisTemplate<String, Object> redisTemplate;
	private RedisKeyGenerator redisKeyGenerator;

	private ModelCloningStrategy modelCloningStrategy;

	@Override
	public PromotionOrderResults getPromotionResults(final AbstractOrderModel order)
	{
		fixCartModel(order);

		final Set<PromotionResultModel> promotionResultModels = order.getAllPromotionResults();

		final List promotionResults = getModelService().getAllSources(promotionResultModels, new ArrayList());

		return new PromotionOrderResults(JaloSession.getCurrentSession().getSessionContext(),
				(AbstractOrder) getModelService().getSource(order), promotionResults, 0.0D);
	}

	@Override
	public PromotionOrderResults getPromotionResults(final Collection<PromotionGroupModel> promotionGroups,
			final AbstractOrderModel order, final boolean evaluateRestrictions, final AutoApplyMode productPromotionMode,
			final AutoApplyMode orderPromotionMode, final Date date)
	{
		fixCartModel(order);
		return super.getPromotionResults(promotionGroups, order, evaluateRestrictions, productPromotionMode, orderPromotionMode,
				date);
	}

	@Override
	public void cleanupCart(final CartModel cart)
	{
		fixCartModel(cart);
		super.cleanupCart(cart);
	}

	@Override
	public void transferPromotionsToOrder(final AbstractOrderModel source, final OrderModel target,
			final boolean onlyTransferAppliedPromotions)
	{
		fixCartModel(source);
		fixPromotionResultforOrder(source);
		super.transferPromotionsToOrder(source, target, onlyTransferAppliedPromotions);
	}

	private void fixCartModel(final AbstractOrderModel model)
	{
		if (!model.getItemtype().equals(RedisCartModel._TYPECODE))
		{
			return; // Do nothing if it is not redis cart.
		}
		final String prPattern = getRedisKeyGenerator().generateCartKey((CartModel) model);
		final Set prKeys = getRedisTemplate().keys(prPattern + ":promotionResult:*");
		final Set promotionResultModels = new HashSet();
		if (CollectionUtils.isNotEmpty(prKeys))
		{
			for (final Object prKey : prKeys)
			{
				final Object prObj = getRedisTemplate().opsForValue().get(prKey);
				if (prObj instanceof PromotionResult)
				{
					final CachedPromotionResultModel promotionResultModel = getModelService().get(prObj);
					promotionResultModels.add(promotionResultModel);
				}
			}
		}
		model.setAllPromotionResults(promotionResultModels);
	}

	private void fixPromotionResultforOrder(final AbstractOrderModel model)
	{
		if (!model.getItemtype().equals(RedisCartModel._TYPECODE))
		{
			return; // Do nothing if it is not redis cart.
		}
		final Collection<PromotionResultModel> allPromotions = model.getAllPromotionResults();
		final Set<PromotionResultModel> allToSavepromotions = new HashSet<PromotionResultModel>();
		for (final PromotionResultModel promotionResult : allPromotions)
		{
			if (promotionResult.getItemtype().equals(CachedPromotionResultModel._TYPECODE))
			{
				final PromotionResultModel toSavePrModel = modelCloningStrategy.clone(promotionResult, PromotionResultModel._TYPECODE,
						getCloneContext());

				final CachedPromotionResultModel cachedPromotionResult = (CachedPromotionResultModel) promotionResult;
				final Collection<AbstractPromotionActionModel> actions = cachedPromotionResult.getCachedActions();
				final Set<AbstractPromotionActionModel> toSaveActions = new HashSet<AbstractPromotionActionModel>();
				for (final AbstractPromotionActionModel abstractPromotionAction : actions)
				{
					AbstractPromotionActionModel toSavePaModel;
					switch (abstractPromotionAction.getItemtype())
					{
						case RedisRuleBasedOrderAddProductActionModel._TYPECODE:
							toSavePaModel = modelCloningStrategy.clone(abstractPromotionAction,
									RuleBasedOrderAddProductActionModel._TYPECODE, getCloneContext());
							break;
						case RedisRuleBasedOrderAdjustTotalActionModel._TYPECODE:
							toSavePaModel = modelCloningStrategy.clone(abstractPromotionAction,
									RuleBasedOrderAdjustTotalActionModel._TYPECODE, getCloneContext());
							break;
						case RedisRuleBasedOrderChangeDeliveryModeActionModel._TYPECODE:
							toSavePaModel = modelCloningStrategy.clone(abstractPromotionAction,
									RuleBasedOrderChangeDeliveryModeActionModel._TYPECODE, getCloneContext());
							break;
						case RedisRuleBasedOrderEntryAdjustActionModel._TYPECODE:
							toSavePaModel = modelCloningStrategy.clone(abstractPromotionAction,
									RuleBasedOrderEntryAdjustActionModel._TYPECODE, getCloneContext());
							break;
						case RedisRuleBasedPotentialPromotionMessageActionModel._TYPECODE:
							toSavePaModel = modelCloningStrategy.clone(abstractPromotionAction,
									RuleBasedPotentialPromotionMessageActionModel._TYPECODE, getCloneContext());
							break;
						default:
							toSavePaModel = null;
							break;
					}
					toSaveActions.add(toSavePaModel);
				}
				toSavePrModel.setActions(toSaveActions);
				toSavePrModel.setAllPromotionActions(toSaveActions);
				final Collection<PromotionOrderEntryConsumedModel> cachedConsumedEntries = cachedPromotionResult.getConsumedEntries();
				final Collection<PromotionOrderEntryConsumedModel> toSaveConsumedEntries = new ArrayList<PromotionOrderEntryConsumedModel>();
				for (final PromotionOrderEntryConsumedModel cachedConsumedEntry : cachedConsumedEntries)
				{
					if (cachedConsumedEntry.getItemtype().equals(CachedPromotionOrderEntryConsumedModel._TYPECODE))
					{
						final PromotionOrderEntryConsumedModel toSaveCeModel = modelCloningStrategy.clone(cachedConsumedEntry,
								PromotionOrderEntryConsumedModel._TYPECODE, getCloneContext());
						toSaveConsumedEntries.add(toSaveCeModel);
					}
				}
				toSavePrModel.setConsumedEntries(toSaveConsumedEntries);

				allToSavepromotions.add(toSavePrModel);
			}
			else
			{
				allToSavepromotions.add(promotionResult);
			}
		}
		model.setAllPromotionResults(allToSavepromotions);
	}

	private ModelCloningContext getCloneContext()
	{
		final ModelCloningContext context = new ModelCloningContext()
		{
			@Override
			public boolean skipAttribute(final Object original, final String qualifier)
			{
				return false;
			}

			@Override
			public boolean treatAsPartOf(final Object original, final String qualifier)
			{
				return false;
			}

			@Override
			public boolean usePresetValue(final Object original, final String qualifier)
			{
				return false;
			}

			@Override
			public Object getPresetValue(final Object original, final String qualifier)
			{
				return null;
			}
		};
		return context;
	}

	/**
	 * @return the redisTemplate
	 */
	public RedisTemplate<String, Object> getRedisTemplate()
	{
		return redisTemplate;
	}

	/**
	 * @param redisTemplate
	 *           the redisTemplate to set
	 */
	public void setRedisTemplate(final RedisTemplate<String, Object> redisTemplate)
	{
		this.redisTemplate = redisTemplate;
	}

	/**
	 * @return the redisKeyGenerator
	 */
	public RedisKeyGenerator getRedisKeyGenerator()
	{
		return redisKeyGenerator;
	}

	/**
	 * @param redisKeyGenerator
	 *           the redisKeyGenerator to set
	 */
	public void setRedisKeyGenerator(final RedisKeyGenerator redisKeyGenerator)
	{
		this.redisKeyGenerator = redisKeyGenerator;
	}

	/**
	 * @return the modelCloningStrategy
	 */
	public ModelCloningStrategy getModelCloningStrategy()
	{
		return modelCloningStrategy;
	}

	/**
	 * @param modelCloningStrategy
	 *           the modelCloningStrategy to set
	 */
	public void setModelCloningStrategy(final ModelCloningStrategy modelCloningStrategy)
	{
		this.modelCloningStrategy = modelCloningStrategy;
	}
}
