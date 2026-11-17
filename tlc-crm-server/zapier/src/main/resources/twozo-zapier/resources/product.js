const PAGE_SIZE = 50;

const productList = async (z, bundle) => {
    const response = await z.request({
        method: 'POST',
        url: 'https://crm.twozo.io/api/zap/dropdown',
        headers: {
          'Authorization': `Bearer ${bundle.authData.access_token}`,
          'X-EMAIL': bundle.authData.email,
          'Content-Type': 'application/json'
        },
        body: {
            page: bundle.meta.page,
            name: 'productList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((prod) => ({
    id: prod.id.toString(),
    name: prod.name
  }));
  };
  
  module.exports = {
    key: 'product_list',
    noun: 'Product List',
    list: {
        display: {
          label: 'List Products',
          description: 'Fetches a list of products for dropdown.',
          hidden: true
        },
        operation: {
          perform: productList,
          canPaginate: true
        },
      },
  };
  