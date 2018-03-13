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
package com.sap.rediscart.util;

import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.promotions.model.PromotionResultModel;

import org.apache.commons.lang3.StringUtils;


/**
 * @author Henter Liu (henterji@163.com)
 */
public class RedisKeyGenerator
{
	public String generateCartKey(final String cartId)
	{
		if (StringUtils.isNoneBlank(cartId))
		{
			return "cart:" + cartId;
		}

		return null;
	}

	public String generateCartKey(final CartModel cart)
	{
		if (cart == null)
		{
			return null;
		}
		return generateCartKey(cart.getCode());
	}

	public String generatePromotionResultKey(final String cartId, final String prPk)
	{
		final String cartKey = generateCartKey(cartId);
		return cartKey + ":promotionResult:" + prPk;
	}

	public String generatePromotionResultKey(final CartModel cart, final PromotionResultModel promotionResult)
	{
		if (cart == null)
		{
			return null;
		}
		if (promotionResult == null)
		{
			return null;
		}
		return generatePromotionResultKey(cart.getCode(), promotionResult.getPk().toString());
	}

	public String generateCodeKey(final String code)
	{
		if (StringUtils.isNoneBlank(code))
		{
			return "cart:code:" + code;
		}

		return null;
	}

	public String generateUserIdKey(final String userId)
	{
		if (StringUtils.isNoneBlank(userId))
		{
			return "cart:userId:" + userId;
		}

		return null;
	}

	public String generateGuidKey(final String guid)
	{
		if (StringUtils.isNoneBlank(guid))
		{
			return "cart:guid:" + guid;
		}

		return null;
	}

	public String generateSiteIdKey(final String siteId)
	{
		if (StringUtils.isNoneBlank(siteId))
		{
			return "cart:siteId:" + siteId;
		}

		return null;
	}
}
