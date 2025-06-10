{
    description = "Network Bans";

    inputs = {
        nixpkgs.url = "nixpkgs/nixos-unstable";

        flake-utils = {
            url = "github:numtide/flake-utils";
            inputs.nixpkgs.follows = "nixpkgs";
        };
    };

    outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
        let pkgs = nixpkgs.legacyPackages.${system}; in {
            devShell = pkgs.mkShell {
                name = "network-bans";
                packages = with pkgs; [
                    gradle
                ];
            };
        }
    );
}
