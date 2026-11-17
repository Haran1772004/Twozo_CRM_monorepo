const PAGE_SIZE = 50;

const userList = async (z, bundle) => {
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
            name: 'userList',
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
    key: 'user_list',
    noun: 'User List',
    list: {
        display: {
          label: 'List Users',
          description: 'Fetches a list of users for dropdown.',
          hidden: true
        },
        operation: {
          perform: userList,
          canPaginate: true
        },
      },
  };
  