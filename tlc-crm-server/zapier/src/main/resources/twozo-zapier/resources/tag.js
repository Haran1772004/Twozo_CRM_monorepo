const PAGE_SIZE = 50;

const tagList = async (z, bundle) => {
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
            name: 'tagList',
            search: bundle.meta.searchTerm || '',
            limit: PAGE_SIZE
        }
    });
  
    const list = response.data?.data?.list || [];

  return list.map((result) => ({
    id: result.id.toString(),
    name: result.name
  }));
  };
  
  module.exports = {
    key: 'tag_list',
    noun: 'Tag List',
    list: {
        display: {
          label: 'List Tags',
          description: 'Fetches a list of tags for dropdown.',
          hidden: true
        },
        operation: {
          perform: tagList,
          canPaginate: true
        },
      },
  };
  