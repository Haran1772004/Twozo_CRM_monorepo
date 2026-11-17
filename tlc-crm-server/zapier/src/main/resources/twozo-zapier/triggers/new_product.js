'use strict';

const subscribeHook = async (z, bundle) => {
  // Step 1: Subscribe to the webhook (POST with zapId)
  const response = await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/event/webhook',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
      type: 'subscribe',
      zapId: bundle.meta.zap.id, 
      hookUrl: bundle.targetUrl,  
      event: 10
    },
  });
  return {
    subscriptionId: response.data.data.id
  };
};

const unsubscribeHook = async (z, bundle) => {
  // Step 2: Unsubscribe (POST with zapId)
  await z.request({
    method: 'POST',
    url: 'https://crm.twozo.io/api/zap/event/webhook',
    headers: {
      'Authorization': `Bearer ${bundle.authData.access_token}`,
      'X-EMAIL': bundle.authData.email,
      'Content-Type': 'application/json'
    },
    body: {
      type: 'unsubscribe',
      id: bundle.subscribeData.subscriptionId,
      zapId: bundle.meta.zap.id, 
      event: 10
    },
  });
  return { status: 'success' };
};

const getProduct = async (z, bundle) => {
  // Step 3: Handle incoming webhook data (optional)
  return [bundle.cleanedRequest];
};

const sampleList = async () => {
    return [
      {
        id: 1,
        name: 'Albotinx'
      },
      {
        id: 2,
        name: 'Infibyn'
      }
    ];
  };

module.exports = {
  key: 'new_product',
  noun: 'Product added',
  display: {
    label: 'New Product Added',
    description: 'Triggers when a new product is added.',
  },
  operation: {
    type: 'hook',
    performSubscribe: subscribeHook,
    performUnsubscribe: unsubscribeHook,
    perform: getProduct,
    performList: sampleList,
    sample: {name: 'Albotinx'},
  },
};