package com.kwonpop.companylife.modules.hr

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HrService {
    private val employees = ConcurrentHashMap<UUID, Employee>()

    fun hire(uuid: UUID, companyId: Long, salary: Double): Employee {
        val employee = Employee(uuid, companyId, salary, Instant.now())
        employees[uuid] = employee
        return employee
    }

    fun fire(uuid: UUID): Employee? = employees.remove(uuid)

    fun list(companyId: Long): List<Employee> = employees.values.filter { it.companyId == companyId }
}

data class Employee(val uuid: UUID, val companyId: Long, val salary: Double, val hiredAt: Instant)
