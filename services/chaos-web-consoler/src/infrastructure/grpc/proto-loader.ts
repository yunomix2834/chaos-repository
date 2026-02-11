import * as path from "path";
import * as protoLoader from "@grpc/proto-loader";
import { loadPackageDefinition } from "@grpc/grpc-js";

export function loadArenaProto(protoPath: string) {
    const abs = path.isAbsolute(protoPath) ? protoPath : path.join(process.cwd(), protoPath);

    const pkgDef = protoLoader.loadSync(abs, {
        keepCase: true,
        longs: String,
        enums: String,
        defaults: true,
        oneofs: true
    })

    return loadPackageDefinition(pkgDef) as any;
}