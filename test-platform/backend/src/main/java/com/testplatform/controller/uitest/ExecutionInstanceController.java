package com.testplatform.controller.uitest;

import com.testplatform.dto.uitest.CreateInstanceRequest;
import com.testplatform.dto.uitest.ExecutionInstanceDto;
import com.testplatform.service.uitest.ExecutionInstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ui-test/instances")
public class ExecutionInstanceController {

    private final ExecutionInstanceService instanceService;

    public ExecutionInstanceController(ExecutionInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    /**
     * 查询所有执行实例：
     * 返回当前可用的执行环境列表（如本地浏览器、远程 WebDriver 等）。
     */
    @GetMapping
    public List<ExecutionInstanceDto> list() {
        return instanceService.listAll();
    }

    /**
     * 创建执行实例：
     * 定义一个新的执行环境，包括名称、类型、远程地址及配置。
     */
    @PostMapping
    public ExecutionInstanceDto create(@RequestBody CreateInstanceRequest req) {
        return instanceService.create(req);
    }
}

