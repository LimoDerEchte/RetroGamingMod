const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const shared_mod = b.createModule(.{
        .root_source_file = b.path("src/shared/shared_structs.zig"),
        .target = target,
        .optimize = optimize,
    });

    const bridge_mod = b.createModule(.{
        .root_source_file = b.path("src/bridge/root.zig"),
        .target = target,
        .optimize = optimize,
    });

    const core_mod = b.createModule(.{
        .root_source_file = b.path("src/retrocore/main.zig"),
        .target = target,
        .optimize = optimize,
    });

    bridge_mod.addImport("shared", shared_mod);
    core_mod.addImport("shared", shared_mod);

    const shared = b.addLibrary(.{
        .linkage = .static,
        .name = "shared",
        .root_module = shared_mod,
    });
    b.installArtifact(shared);

    const lib = b.addLibrary(.{
        .linkage = .dynamic,
        .name = "bridge",
        .root_module = bridge_mod,
    });
    lib.linkLibC();
    lib.addIncludePath(std.Build.path(b, "lib/enet/include"));
    lib.addCSourceFiles(.{
        .root = std.Build.path(b, "lib/enet"),
        .files = &.{
            "callbacks.c",
            "compress.c",
            "host.c",
            "list.c",
            "packet.c",
            "peer.c",
            "protocol.c",
            "unix.c",
            "win32.c",
        }
    });
    b.installArtifact(lib);

    const exe = b.addExecutable(.{
        .name = "retrocore",
        .root_module = core_mod,
    });
    b.installArtifact(exe);

    const run_cmd = b.addRunArtifact(exe);
    run_cmd.step.dependOn(b.getInstallStep());

    if (b.args) |args| {
        run_cmd.addArgs(args);
    }

    const run_step = b.step("run", "Run the app");
    run_step.dependOn(&run_cmd.step);

    const lib_unit_tests = b.addTest(.{
        .root_module = bridge_mod,
    });

    const run_lib_unit_tests = b.addRunArtifact(lib_unit_tests);

    const exe_unit_tests = b.addTest(.{
        .root_module = core_mod,
    });

    const run_exe_unit_tests = b.addRunArtifact(exe_unit_tests);

    const test_step = b.step("test", "Run unit tests");
    test_step.dependOn(&run_lib_unit_tests.step);
    test_step.dependOn(&run_exe_unit_tests.step);
}
