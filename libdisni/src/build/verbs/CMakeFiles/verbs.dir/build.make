# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.25

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Disable VCS-based implicit rules.
% : %,v

# Disable VCS-based implicit rules.
% : RCS/%

# Disable VCS-based implicit rules.
% : RCS/%,v

# Disable VCS-based implicit rules.
% : SCCS/s.%

# Disable VCS-based implicit rules.
% : s.%

.SUFFIXES: .hpux_make_needs_suffix_list

# Command-line flag to silence nested $(MAKE).
$(VERBOSE)MAKESILENT = -s

#Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /Applications/CMake.app/Contents/bin/cmake

# The command to remove a file.
RM = /Applications/CMake.app/Contents/bin/cmake -E rm -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build

# Include any dependencies generated for this target.
include verbs/CMakeFiles/verbs.dir/depend.make
# Include any dependencies generated by the compiler for this target.
include verbs/CMakeFiles/verbs.dir/compiler_depend.make

# Include the progress variables for this target.
include verbs/CMakeFiles/verbs.dir/progress.make

# Include the compile flags for this target's objects.
include verbs/CMakeFiles/verbs.dir/flags.make

verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o: verbs/CMakeFiles/verbs.dir/flags.make
verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o: /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/verbs/com_ibm_disni_verbs_impl_NativeDispatcher.cpp
verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o: verbs/CMakeFiles/verbs.dir/compiler_depend.ts
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o"
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && /Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -MD -MT verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o -MF CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o.d -o CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o -c /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/verbs/com_ibm_disni_verbs_impl_NativeDispatcher.cpp

verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.i"
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && /Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/verbs/com_ibm_disni_verbs_impl_NativeDispatcher.cpp > CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.i

verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.s"
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && /Library/Developer/CommandLineTools/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/verbs/com_ibm_disni_verbs_impl_NativeDispatcher.cpp -o CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.s

# Object files for target verbs
verbs_OBJECTS = \
"CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o"

# External object files for target verbs
verbs_EXTERNAL_OBJECTS =

verbs/libverbs.a: verbs/CMakeFiles/verbs.dir/com_ibm_disni_verbs_impl_NativeDispatcher.cpp.o
verbs/libverbs.a: verbs/CMakeFiles/verbs.dir/build.make
verbs/libverbs.a: verbs/CMakeFiles/verbs.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking CXX static library libverbs.a"
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && $(CMAKE_COMMAND) -P CMakeFiles/verbs.dir/cmake_clean_target.cmake
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && $(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/verbs.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
verbs/CMakeFiles/verbs.dir/build: verbs/libverbs.a
.PHONY : verbs/CMakeFiles/verbs.dir/build

verbs/CMakeFiles/verbs.dir/clean:
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs && $(CMAKE_COMMAND) -P CMakeFiles/verbs.dir/cmake_clean.cmake
.PHONY : verbs/CMakeFiles/verbs.dir/clean

verbs/CMakeFiles/verbs.dir/depend:
	cd /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/verbs /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs /Users/nanachi/VVUUAA/repository/disni_mae/disni/libdisni/src/build/verbs/CMakeFiles/verbs.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : verbs/CMakeFiles/verbs.dir/depend

