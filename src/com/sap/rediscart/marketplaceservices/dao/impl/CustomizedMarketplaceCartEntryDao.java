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
package com.sap.rediscart.marketplaceservices.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.commerceservices.order.dao.impl.DefaultCartEntryDao;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.marketplaceservices.dao.MarketplaceCartEntryDao;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class CustomizedMarketplaceCartEntryDao extends DefaultCartEntryDao implements MarketplaceCartEntryDao
{
	@Override
	public List<CartEntryModel> findUnSaleableCartEntries(final CartModel cart)
	{
		validateParameterNotNull(cart, "Cart must not be null");
		final List<AbstractOrderEntryModel> entries = cart.getEntries();
		final List<CartEntryModel> results = new ArrayList<>();
		for (final AbstractOrderEntryModel entry : entries)
		{
			final ProductModel product = entry.getProduct();
			final Boolean saleable = product.getSaleable();
			if (saleable.equals(Boolean.FALSE))
			{
				results.add((CartEntryModel) entry);
			}
		}
		return results;
	}
}
