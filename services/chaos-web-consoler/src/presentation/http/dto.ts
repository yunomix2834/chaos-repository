import { IsInt, IsNotEmpty, IsOptional, IsString, Min } from "class-validator";

export class SubmitCommandDto {
    @IsString() @IsNotEmpty()
    arenaId!: string;

    @IsString() @IsNotEmpty()
    taskId!: string;

    @IsString() @IsNotEmpty()
    type!: "SCALE" | "KILL_PODS" | "ROLLBACK";

    @IsString() @IsNotEmpty()
    target!: string;

    @IsInt()
    value!: number;

    @IsString() @IsOptional()
    reason?: string;

    @IsString() @IsOptional()
    requestedBy?: string;
}

export class SubmitAndWaitDto extends SubmitCommandDto {
    @IsInt() @Min(100)
    waitTimeoutMs!: number;
}
