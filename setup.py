from pathlib import Path

from setuptools import find_packages, setup

BASE_DIR = Path(__file__).parent
README = (BASE_DIR / "README.md").read_text(encoding="utf-8")

setup(
    name="advanced_curses",
    version="0.1.0",
    description="High-level curses wrapper with event loop, layouts, widgets, and theming",
    long_description=README,
    long_description_content_type="text/markdown",
    author="Advanced Curses Contributors",
    url="https://example.com/advanced_curses",
    packages=find_packages(exclude=("tests", "examples")),
    include_package_data=True,
    python_requires=">=3.9",
    install_requires=[],
    extras_require={
        "windows": ["windows-curses>=2.3.0"],
    },
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "License :: OSI Approved :: MIT License",
        "Operating System :: POSIX",
        "Operating System :: Microsoft :: Windows",
        "Environment :: Console",
        "Environment :: Console :: Curses",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: Software Development :: User Interfaces",
    ],
)
