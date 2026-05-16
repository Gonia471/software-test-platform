export const seedApiTestData = () => {
  const collections = [
    {
      id: 'folder-user-mgmt',
      parentId: 'root',
      name: '用户管理',
      type: 'folder',
      children: [
        {
          id: 'case-get-users',
          parentId: 'folder-user-mgmt',
          name: '获取用户列表',
          type: 'case',
          method: 'GET',
          url: 'https://jsonplaceholder.typicode.com/users',
          params: [
            { key: 'page', value: '1', enabled: true },
            { key: 'limit', value: '10', enabled: true }
          ],
          headers: [
            { key: 'Accept', value: 'application/json', enabled: true }
          ],
          bodyType: 'none',
          bodyRaw: '',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.length', expected: '>0', enabled: true }
          ]
        },
        {
          id: 'case-get-user-by-id',
          parentId: 'folder-user-mgmt',
          name: '获取单个用户',
          type: 'case',
          method: 'GET',
          url: 'https://jsonplaceholder.typicode.com/users/1',
          params: [],
          headers: [
            { key: 'Accept', value: 'application/json', enabled: true }
          ],
          bodyType: 'none',
          bodyRaw: '',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.id', expected: '==1', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.username', expected: 'Bret', enabled: true }
          ]
        },
        {
          id: 'case-create-user',
          parentId: 'folder-user-mgmt',
          name: '创建用户',
          type: 'case',
          method: 'POST',
          url: 'https://jsonplaceholder.typicode.com/users',
          params: [],
          headers: [
            { key: 'Content-Type', value: 'application/json', enabled: true },
            { key: 'Accept', value: 'application/json', enabled: true }
          ],
          bodyType: 'raw',
          bodyRaw: '{"name":"张三","username":"zhangsan","email":"zhangsan@example.com","phone":"1234567890"}',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '201', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.name', expected: '张三', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.id', expected: '>0', enabled: true }
          ]
        },
        {
          id: 'case-update-user',
          parentId: 'folder-user-mgmt',
          name: '更新用户',
          type: 'case',
          method: 'PUT',
          url: 'https://jsonplaceholder.typicode.com/users/1',
          params: [],
          headers: [
            { key: 'Content-Type', value: 'application/json', enabled: true }
          ],
          bodyType: 'raw',
          bodyRaw: '{"name":"李四更新","username":"lisi_updated","email":"lisi@example.com"}',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.name', expected: '李四更新', enabled: true }
          ]
        },
        {
          id: 'case-delete-user',
          parentId: 'folder-user-mgmt',
          name: '删除用户',
          type: 'case',
          method: 'DELETE',
          url: 'https://jsonplaceholder.typicode.com/users/1',
          params: [],
          headers: [],
          bodyType: 'none',
          bodyRaw: '',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true }
          ]
        }
      ]
    },
    {
      id: 'folder-posts',
      parentId: 'root',
      name: '文章管理',
      type: 'folder',
      children: [
        {
          id: 'case-get-posts',
          parentId: 'folder-posts',
          name: '获取文章列表',
          type: 'case',
          method: 'GET',
          url: 'https://jsonplaceholder.typicode.com/posts',
          params: [
            { key: 'userId', value: '1', enabled: true }
          ],
          headers: [],
          bodyType: 'none',
          bodyRaw: '',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.length', expected: '>0', enabled: true }
          ]
        },
        {
          id: 'case-get-post-comments',
          parentId: 'folder-posts',
          name: '获取文章评论',
          type: 'case',
          method: 'GET',
          url: 'https://jsonplaceholder.typicode.com/posts/1/comments',
          params: [],
          headers: [],
          bodyType: 'none',
          bodyRaw: '',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '200', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.length', expected: '>0', enabled: true },
            { assertionType: 'JSONPATH', expression: '$[0].email', expected: 'includes@', enabled: true }
          ]
        },
        {
          id: 'case-create-post',
          parentId: 'folder-posts',
          name: '创建文章',
          type: 'case',
          method: 'POST',
          url: 'https://jsonplaceholder.typicode.com/posts',
          params: [],
          headers: [
            { key: 'Content-Type', value: 'application/json', enabled: true }
          ],
          bodyType: 'raw',
          bodyRaw: '{"title":"测试文章标题","body":"这是文章内容","userId":1}',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'none',
          authConfig: {},
          prescripts: [],
          assertions: [
            { assertionType: 'STATUS', expression: '201', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.title', expected: '测试文章标题', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.id', expected: '>0', enabled: true }
          ]
        }
      ]
    },
    {
      id: 'folder-full-flow',
      parentId: 'root',
      name: '完整流程示例',
      type: 'folder',
      children: [
        {
          id: 'case-full-workflow',
          parentId: 'folder-full-flow',
          name: '用户完整操作流程',
          type: 'case',
          method: 'POST',
          url: 'https://jsonplaceholder.typicode.com/users',
          params: [],
          headers: [
            { key: 'Content-Type', value: 'application/json', enabled: true },
            { key: 'X-Request-ID', value: '{{request_id}}', enabled: true }
          ],
          bodyType: 'raw',
          bodyRaw: '{"name":"新用户{{timestamp}}","username":"newuser{{random}}","email":"user{{random}}@example.com"}',
          bodyRawType: 'json',
          bodyForm: [],
          authType: 'bearer',
          authConfig: { token: 'Bearer {{api_token}}' },
          prescripts: [
            {
              stepType: 'SET_VARIABLE',
              varName: 'request_id',
              varValue: '{{uuid()}}',
              stopOnFail: false
            },
            {
              stepType: 'SET_VARIABLE',
              varName: 'timestamp',
              varValue: '{{timestamp()}}',
              stopOnFail: false
            },
            {
              stepType: 'SET_VARIABLE',
              varName: 'random',
              varValue: '{{randomInt(1000,9999)}}',
              stopOnFail: false
            },
            {
              stepType: 'HTTP',
              method: 'GET',
              url: 'https://jsonplaceholder.typicode.com/users/1',
              extractParams: [
                { name: 'existing_username', path: '$.username' },
                { name: 'existing_email', path: '$.email' }
              ],
              assertions: [
                { assertionType: 'STATUS', expression: '200', expected: '', enabled: true }
              ],
              stopOnFail: false
            },
            {
              stepType: 'FUNCTION',
              functionName: 'validate_email',
              functionParams: 'user@example.com',
              outputVar: 'email_valid',
              stopOnFail: false
            }
          ],
          assertions: [
            { assertionType: 'STATUS', expression: '201', expected: '', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.id', expected: '>0', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.name', expected: 'includes新用户', enabled: true },
            { assertionType: 'JSONPATH', expression: '$.username', expected: 'startsWithnewuser', enabled: true },
            { assertionType: 'CONTAINS', expression: '$.email', expected: '@example.com', enabled: true },
            { assertionType: 'DURATION', expression: '', expected: '5000', enabled: true }
          ]
        }
      ]
    }
  ];

  const environments = [
    {
      id: 'env-jsonplaceholder',
      name: 'JSONPlaceholder',
      variables: [
        { key: 'base_url', value: 'https://jsonplaceholder.typicode.com' },
        { key: 'api_token', value: 'test-token-12345' },
        { key: 'request_id', value: '' }
      ]
    },
    {
      id: 'env-dev',
      name: '开发环境',
      variables: [
        { key: 'base_url', value: 'http://localhost:8080' },
        { key: 'api_token', value: 'dev-token-xxx' }
      ]
    }
  ];

  const existingData = localStorage.getItem('api-test-collections-v1');
  if (existingData) {
    const parsed = JSON.parse(existingData);
    const root = parsed.find(n => n.id === 'root');
    if (root) {
      collections.forEach(col => root.children.push(col));
    }
  } else {
    parsed = [
      { id: 'root', name: '我的接口', type: 'folder', children: [] },
      ...collections
    ];
  }

  localStorage.setItem('api-test-collections-v1', JSON.stringify(parsed));

  const existingEnvs = localStorage.getItem('api-test-environments-v1');
  if (existingEnvs) {
    const parsedEnvs = JSON.parse(existingEnvs);
    const envIds = parsedEnvs.map(e => e.id);
    environments.forEach(env => {
      if (!envIds.includes(env.id)) {
        parsedEnvs.push(env);
      }
    });
    localStorage.setItem('api-test-environments-v1', JSON.stringify(parsedEnvs));
  } else {
    localStorage.setItem('api-test-environments-v1', JSON.stringify(environments));
  }

  console.log('✅ API测试示例数据已添加！');
  console.log('📁 已创建集合：');
  console.log('   - 用户管理 (5个接口)');
  console.log('   - 文章管理 (3个接口)');
  console.log('   - 完整流程示例 (1个完整用例)');
  console.log('');
  console.log('🌍 已添加环境：JSONPlaceholder');
  console.log('');
  console.log('请刷新页面查看数据。');

  return { collections, environments };
};

export const clearAndSeedData = () => {
  localStorage.removeItem('api-test-collections-v1');
  localStorage.removeItem('api-test-environments-v1');
  localStorage.removeItem('api-test-current-env-v1');
  localStorage.removeItem('api-test-request-history-v1');
  seedApiTestData();
};
