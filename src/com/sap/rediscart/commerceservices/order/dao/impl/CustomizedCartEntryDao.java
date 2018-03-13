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
package com.sap.rediscart.commerceservices.order.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.commerceservices.order.dao.CartEntryDao;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.storelocator.model.PointOfServiceModel;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedCartEntryDao implements CartEntryDao
{
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(CustomizedCartEntryDao.class);

	@Override
	public List<CartEntryModel> findEntriesByProductAndPointOfService(final CartModel cart, final ProductModel product,
			final PointOfServiceModel pointOfService)
	{
		validateParameterNotNull(cart, "cart must not be null");
		validateParameterNotNull(product, "product must not be null");

		final List<CartEntryModel> cartEntries = new ArrayList<>();
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		for (final AbstractOrderEntryModel abstractOrderEntryModel : entries)
		{
			final ProductModel productModel = abstractOrderEntryModel.getProduct();
			final PointOfServiceModel pointOfServiceModel = abstractOrderEntryModel.getDeliveryPointOfService();
			if (pointOfService == null)
			{
				if (product.getPk().equals(productModel.getPk()) && pointOfServiceModel == null)
				{
					cartEntries.add((CartEntryModel) abstractOrderEntryModel);
				}
			}
			else
			{
				if (product.getPk().equals(productModel.getPk()) && pointOfService.getPk().equals(pointOfServiceModel.getPk()))
				{
					cartEntries.add((CartEntryModel) abstractOrderEntryModel);
				}
			}
		}
		return cartEntries;
	}
}
