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

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static java.util.Objects.isNull;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.order.daos.DeliveryModeDao;
import de.hybris.platform.promotionengineservices.model.RuleBasedOrderChangeDeliveryModeActionModel;
import de.hybris.platform.promotions.model.PromotionResultModel;
import de.hybris.platform.ruleengineservices.rao.AbstractRuleActionRAO;
import de.hybris.platform.ruleengineservices.rao.CartRAO;
import de.hybris.platform.ruleengineservices.rao.ShipmentRAO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;


/**
 * Encapsulates logic of Rule Action processing for setting Delivery options as a Promotion Action.
 *
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedShippingActionStrategy extends CustomizedRuleActionStrategy<RuleBasedOrderChangeDeliveryModeActionModel>
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomizedShippingActionStrategy.class);

	private DeliveryModeDao deliveryModeDao;

	/**
	 * If the parameter action is of type {@link ShipmentRAO} Delivery options set to corresponding fields of Order model.
	 *
	 * @return empty list since no any PromotionResultModel can correspond to Shipment Promotion Action.
	 */
	@Override
	public List<PromotionResultModel> apply(final AbstractRuleActionRAO action)
	{
		if (!(action instanceof ShipmentRAO))
		{
			LOG.error("cannot apply {}, action is not of type ShipmentRAO, but {}", getClass().getSimpleName(), action);
			return Collections.emptyList();
		}
		final ShipmentRAO changeDeliveryMethodAction = (ShipmentRAO) action;
		if (!(changeDeliveryMethodAction.getAppliedToObject() instanceof CartRAO))
		{
			LOG.error("cannot apply {}, appliedToObject is not of type CartRAO, but {}", getClass().getSimpleName(),
					action.getAppliedToObject());
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

		final ShipmentRAO shipmentRAO = (ShipmentRAO) action;
		final DeliveryModeModel shipmentModel = getDeliveryModeForCode(shipmentRAO.getMode().getCode());
		if (shipmentModel == null)
		{
			LOG.error("Delivery Mode for code {} not found!", shipmentRAO.getMode());
			return Collections.emptyList();
		}
		final DeliveryModeModel shipmentModelToReplace = order.getDeliveryMode();
		order.setDeliveryMode(shipmentModel);

		final Double deliveryCostToReplace = order.getDeliveryCost();
		order.setDeliveryCost(Double.valueOf(shipmentRAO.getMode().getCost().doubleValue()));

		final RuleBasedOrderChangeDeliveryModeActionModel actionModel = createPromotionAction(promoResult, action);
		handleActionMetadata(action, actionModel);
		actionModel.setDeliveryMode(shipmentModel);
		actionModel.setDeliveryCost(shipmentRAO.getMode().getCost());
		actionModel.setReplacedDeliveryMode(shipmentModelToReplace);
		actionModel.setReplacedDeliveryCost(BigDecimal.valueOf(deliveryCostToReplace.doubleValue()));
		getModelService().saveAll(promoResult, actionModel, order);
		return Collections.singletonList(promoResult);
	}

	@Override
	public void undo(final ItemModel item)
	{
		if (item instanceof RuleBasedOrderChangeDeliveryModeActionModel)
		{
			handleUndoActionMetadata((RuleBasedOrderChangeDeliveryModeActionModel) item);
			final RuleBasedOrderChangeDeliveryModeActionModel action = (RuleBasedOrderChangeDeliveryModeActionModel) item;
			final AbstractOrderModel order = action.getPromotionResult().getOrder();
			order.setDeliveryMode(action.getReplacedDeliveryMode());
			order.setDeliveryCost(Double.valueOf(action.getReplacedDeliveryCost().doubleValue()));
			undoInternal(action);
			getModelService().save(order);
		}
	}

	protected DeliveryModeModel getDeliveryModeForCode(final String code)
	{
		validateParameterNotNull(code, "Parameter code cannot be null");

		final List<DeliveryModeModel> deliveryModes = getDeliveryModeDao().findDeliveryModesByCode(code);

		return CollectionUtils.isNotEmpty(deliveryModes) ? deliveryModes.get(0) : null;
	}

	protected DeliveryModeDao getDeliveryModeDao()
	{
		return deliveryModeDao;
	}

	@Required
	public void setDeliveryModeDao(final DeliveryModeDao deliveryModeDao)
	{
		this.deliveryModeDao = deliveryModeDao;
	}
}
