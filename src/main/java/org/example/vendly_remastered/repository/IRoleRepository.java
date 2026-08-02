package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Role;

import java.util.List;

public interface IRoleRepository {
    public int addRole(Role role);
    public boolean saveRole(Role role);
    public boolean deleteRole(int roleId);
    public List<Role> findAll();
}
