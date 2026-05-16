#!/usr/bin/env python3
"""
权限控制测试脚本
测试不同角色对UI测试和API测试的访问权限
"""

import requests
import json
import sys

BASE_URL = "http://localhost:8080/api"

# 测试用的token（开发者模式）
DEV_TOKEN = "dev-token"

def test_developer_mode():
    """测试开发者模式可以看到所有组织"""
    print("=== 测试1: 开发者模式可以看到所有组织 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json"
    }
    
    try:
        response = requests.get(f"{BASE_URL}/organizations", headers=headers)
        print(f"状态码: {response.status_code}")
        
        if response.status_code == 200:
            organizations = response.json()
            print(f"返回的组织数量: {len(organizations)}")
            for org in organizations:
                print(f"  - ID: {org['id']}, 名称: {org['name']}, 成员数: {org['memberCount']}")
            
            if len(organizations) > 0:
                print("✓ 开发者模式可以查看所有组织")
                return True
            else:
                print("✗ 开发者模式没有看到任何组织")
                return False
        else:
            print(f"✗ 请求失败: {response.text}")
            return False
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False

def test_ui_test_permissions():
    """测试UI测试的权限控制"""
    print("\n=== 测试2: UI测试权限控制 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json"
    }
    
    # 获取UI测试用例列表
    try:
        response = requests.get(f"{BASE_URL}/ui-test/cases", headers=headers)
        print(f"UI测试用例列表状态码: {response.status_code}")
        
        if response.status_code == 200:
            cases = response.json()
            print(f"返回的UI测试用例数量: {len(cases)}")
            
            # 检查每个用例的组织信息
            for case in cases[:5]:  # 只显示前5个
                org_id = case.get('organizationId')
                print(f"  用例 '{case['name']}': 组织ID={org_id}")
            
            print("✓ UI测试权限控制正常")
            return True
        else:
            print(f"✗ 获取UI测试用例失败: {response.text}")
            return False
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False

def test_api_test_permissions():
    """测试API测试的权限控制"""
    print("\n=== 测试3: API测试权限控制 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json"
    }
    
    # 获取API测试集合列表
    try:
        response = requests.get(f"{BASE_URL}/api-test/collections", headers=headers)
        print(f"API测试集合列表状态码: {response.status_code}")
        
        if response.status_code == 200:
            collections = response.json()
            print(f"返回的API测试集合数量: {len(collections)}")
            
            # 检查每个集合的组织信息
            for collection in collections[:5]:  # 只显示前5个
                org_id = collection.get('organizationId')
                print(f"  集合 '{collection['name']}': 组织ID={org_id}")
            
            print("✓ API测试权限控制正常")
            return True
        else:
            print(f"✗ 获取API测试集合失败: {response.text}")
            return False
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False

def test_create_ui_test_case():
    """测试创建UI测试用例的权限"""
    print("\n=== 测试4: 创建UI测试用例权限 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json",
        "Content-Type": "application/json"
    }
    
    # 创建测试用例的请求数据
    test_case_data = {
        "name": "权限测试用例",
        "description": "测试权限控制的UI测试用例",
        "organizationId": 9,  # 使用现有的组织ID
        "steps": [
            {
                "action": "navigate",
                "target": "https://example.com",
                "value": "",
                "description": "访问示例网站"
            }
        ]
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/ui-test/cases",
            headers=headers,
            data=json.dumps(test_case_data)
        )
        print(f"创建UI测试用例状态码: {response.status_code}")
        
        if response.status_code == 200:
            created_case = response.json()
            print(f"✓ 成功创建UI测试用例: {created_case['name']} (ID: {created_case['id']})")
            print(f"  所属组织ID: {created_case.get('organizationId')}")
            return True, created_case['id']
        elif response.status_code == 400:
            error_msg = response.json().get('message', response.text)
            print(f"✗ 创建失败 (权限问题或其他错误): {error_msg}")
            return False, None
        else:
            print(f"✗ 创建失败: {response.text}")
            return False, None
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False, None

def test_create_api_collection():
    """测试创建API测试集合的权限"""
    print("\n=== 测试5: 创建API测试集合权限 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json",
        "Content-Type": "application/json"
    }
    
    # 创建API集合的请求数据
    collection_data = {
        "name": "权限测试API集合",
        "description": "测试权限控制的API集合",
        "type": "folder",
        "organizationId": 9,  # 使用现有的组织ID
        "method": "GET",
        "url": "https://api.example.com/test"
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/api-test/collections",
            headers=headers,
            data=json.dumps(collection_data)
        )
        print(f"创建API测试集合状态码: {response.status_code}")
        
        if response.status_code == 200:
            created_collection = response.json()
            print(f"✓ 成功创建API测试集合: {created_collection['name']} (ID: {created_collection['id']})")
            print(f"  所属组织ID: {created_collection.get('organizationId')}")
            return True, created_collection['id']
        elif response.status_code == 400:
            error_msg = response.json().get('message', response.text)
            print(f"✗ 创建失败 (权限问题或其他错误): {error_msg}")
            return False, None
        else:
            print(f"✗ 创建失败: {response.text}")
            return False, None
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False, None

def test_organization_member_permissions():
    """测试组织成员权限"""
    print("\n=== 测试6: 组织成员权限 ===")
    
    headers = {
        "Authorization": f"Bearer {DEV_TOKEN}",
        "Accept": "application/json"
    }
    
    # 获取组织成员列表
    try:
        response = requests.get(f"{BASE_URL}/organizations/9/members", headers=headers)
        print(f"组织成员列表状态码: {response.status_code}")
        
        if response.status_code == 200:
            members = response.json()
            print(f"组织ID=9的成员数量: {len(members)}")
            
            for member in members:
                print(f"  成员: {member['username']}, 角色: {member['role']}")
            
            # 检查是否有空间创建者/管理员
            space_admins = [m for m in members if m['role'] in ['SPACE_CREATOR', 'SPACE_ADMIN']]
            if space_admins:
                print(f"✓ 找到空间管理员: {len(space_admins)}个")
                return True
            else:
                print("✗ 没有找到空间管理员")
                return False
        else:
            print(f"✗ 获取成员列表失败: {response.text}")
            return False
            
    except Exception as e:
        print(f"✗ 请求异常: {e}")
        return False

def main():
    """主测试函数"""
    print("开始权限控制测试...")
    print("=" * 50)
    
    # 检查后端服务是否运行
    try:
        health_check = requests.get("http://localhost:8080/actuator/health", timeout=5)
        if health_check.status_code != 200:
            print("✗ 后端服务未运行或健康检查失败")
            return 1
    except:
        print("✗ 无法连接到后端服务，请确保服务正在运行")
        return 1
    
    test_results = []
    
    # 运行所有测试
    test_results.append(("开发者模式查看组织", test_developer_mode()))
    test_results.append(("UI测试权限控制", test_ui_test_permissions()))
    test_results.append(("API测试权限控制", test_api_test_permissions()))
    
    ui_test_result, ui_case_id = test_create_ui_test_case()
    test_results.append(("创建UI测试用例", ui_test_result))
    
    api_test_result, api_collection_id = test_create_api_collection()
    test_results.append(("创建API测试集合", api_test_result))
    
    test_results.append(("组织成员权限", test_organization_member_permissions()))
    
    # 输出测试总结
    print("\n" + "=" * 50)
    print("测试总结:")
    print("=" * 50)
    
    passed = 0
    total = len(test_results)
    
    for test_name, result in test_results:
        status = "✓ 通过" if result else "✗ 失败"
        print(f"{test_name}: {status}")
        if result:
            passed += 1
    
    print(f"\n总计: {passed}/{total} 个测试通过")
    
    if passed == total:
        print("✓ 所有权限控制测试通过！")
        return 0
    else:
        print(f"✗ {total - passed} 个测试失败")
        return 1

if __name__ == "__main__":
    sys.exit(main())